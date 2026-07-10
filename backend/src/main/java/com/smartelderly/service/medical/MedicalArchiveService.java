package com.smartelderly.service.medical;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartelderly.api.ApiException;
import com.smartelderly.api.dto.MedicalOcrView;
import com.smartelderly.api.dto.medical.CreateMedicalArchiveFolderRequest;
import com.smartelderly.api.dto.medical.ExtractedMedicalFieldsDto;
import com.smartelderly.api.dto.medical.MedicalDisplayBlockDto;
import com.smartelderly.api.dto.medical.MedicalArchiveFolderViewDto;
import com.smartelderly.api.dto.medical.MedicalDocumentDetailDto;
import com.smartelderly.api.dto.medical.MedicalDocumentSummaryDto;
import com.smartelderly.api.dto.medical.PatchMedicalDocumentRequest;
import com.smartelderly.domain.MedicalArchiveFolder;
import com.smartelderly.domain.MedicalArchiveFolderRepository;
import com.smartelderly.domain.MedicalCalendarEventRepository;
import com.smartelderly.domain.MedicalDocument;
import com.smartelderly.domain.MedicalDocumentRepository;
import com.smartelderly.security.AuthPrincipal;

@Service
public class MedicalArchiveService {

    private static final Logger log = LoggerFactory.getLogger(MedicalArchiveService.class);

    private final MedicalAccessService medicalAccessService;
    private final MedicalArchiveFolderRepository folderRepository;
    private final MedicalCalendarEventRepository medicalCalendarEventRepository;
    private final MedicalDocumentRepository documentRepository;
    private final MedicalDocumentStorageService medicalDocumentStorageService;
    private final MedicalFieldExtractionService medicalFieldExtractionService;
    private final MedicalFieldPostProcessor medicalFieldPostProcessor;
    private final MedicalDocumentRenderService medicalDocumentRenderService;
    private final ObjectMapper objectMapper;

    public MedicalArchiveService(
            MedicalAccessService medicalAccessService,
            MedicalArchiveFolderRepository folderRepository,
            MedicalCalendarEventRepository medicalCalendarEventRepository,
            MedicalDocumentRepository documentRepository,
            MedicalDocumentStorageService medicalDocumentStorageService,
            MedicalFieldExtractionService medicalFieldExtractionService,
            MedicalFieldPostProcessor medicalFieldPostProcessor,
            MedicalDocumentRenderService medicalDocumentRenderService,
            ObjectMapper objectMapper) {
        this.medicalAccessService = medicalAccessService;
        this.folderRepository = folderRepository;
        this.medicalCalendarEventRepository = medicalCalendarEventRepository;
        this.documentRepository = documentRepository;
        this.medicalDocumentStorageService = medicalDocumentStorageService;
        this.medicalFieldExtractionService = medicalFieldExtractionService;
        this.medicalFieldPostProcessor = medicalFieldPostProcessor;
        this.medicalDocumentRenderService = medicalDocumentRenderService;
        this.objectMapper = objectMapper;
    }

    public List<MedicalArchiveFolderViewDto> listFolders(AuthPrincipal principal, Long elderProfileId) {
        long eid = medicalAccessService.resolveTargetElderProfileId(principal, elderProfileId);
        return folderRepository.findByElderProfileIdOrderBySortOrderAscIdAsc(eid).stream()
                .map(MedicalArchiveService::toFolderView)
                .toList();
    }

    public MedicalArchiveFolderViewDto createFolder(
            AuthPrincipal principal, CreateMedicalArchiveFolderRequest req) {
        long eid = medicalAccessService.resolveTargetElderProfileId(principal, req.getElderProfileId());
        String folderName = req.getName().trim();
        if (folderRepository.existsByElderProfileIdAndName(eid, folderName)) {
            throw new ApiException(4002, "病情文件夹已存在");
        }
        int maxSort =
                folderRepository.findByElderProfileIdOrderBySortOrderAscIdAsc(eid).stream()
                        .map(MedicalArchiveFolder::getSortOrder)
                        .max(Integer::compareTo)
                        .orElse(0);
        MedicalArchiveFolder f = new MedicalArchiveFolder();
        f.setElderProfileId(eid);
        f.setName(folderName);
        f.setSortOrder(maxSort + 1);
        f = folderRepository.save(f);
        return toFolderView(f);
    }

    public List<MedicalDocumentSummaryDto> listDocuments(
            AuthPrincipal principal,
            Long elderProfileId,
            Long folderId,
            String docCategory) {
        long eid = medicalAccessService.resolveTargetElderProfileId(principal, elderProfileId);
        List<MedicalDocument> rows;
        if (folderId != null) {
            rows = documentRepository.findByElderProfileIdAndFolderIdOrderByCreatedAtDesc(eid, folderId);
        } else if (docCategory != null && !docCategory.isBlank()) {
            rows =
                    documentRepository.findByElderProfileIdAndDocCategoryOrderByCreatedAtDesc(
                            eid, docCategory.trim());
        } else {
            rows = documentRepository.findByElderProfileIdOrderByCreatedAtDesc(eid);
        }
        Map<Long, String> folderNameById = loadFolderNameById(eid);
        return rows.stream().map(d -> toSummary(d, folderNameById)).toList();
    }

    public MedicalDocumentDetailDto getDocument(AuthPrincipal principal, Long elderProfileId, long documentId) {
        long eid = medicalAccessService.resolveTargetElderProfileId(principal, elderProfileId);
        MedicalDocument d =
                documentRepository
                        .findByIdAndElderProfileId(documentId, eid)
                        .orElseThrow(() -> new ApiException(4041, "单据不存在"));
        return toDetail(d);
    }

    public MedicalDocumentDetailDto patchDocument(
            AuthPrincipal principal,
            Long elderProfileId,
            long documentId,
            PatchMedicalDocumentRequest req) {
        if (req.getTitle() == null
                && req.getFullText() == null
                && req.getDocCategory() == null
                && !req.isFolderIdSpecified()) {
            throw new ApiException(4001, "至少提供一个可更新字段");
        }
        long eid = medicalAccessService.resolveTargetElderProfileId(principal, elderProfileId);
        MedicalDocument d =
                documentRepository
                        .findByIdAndElderProfileId(documentId, eid)
                        .orElseThrow(() -> new ApiException(4041, "单据不存在"));
        if (req.getTitle() != null) {
            String title = req.getTitle().trim();
            if (title.isEmpty()) {
                throw new ApiException(4001, "title 不能为空");
            }
            d.setTitle(title);
        }
        if (req.getFullText() != null) {
            d.setFullText(req.getFullText());
        }
        if (req.getDocCategory() != null) {
            String docCategory = req.getDocCategory().trim();
            if (docCategory.isEmpty()) {
                throw new ApiException(4001, "docCategory 不能为空");
            }
            d.setDocCategory(docCategory.toUpperCase());
        }
        if (req.isFolderIdSpecified()) {
            Long fid = req.getFolderId();
            if (fid == null || fid == 0L) {
                d.setFolderId(null);
            } else {
                folderRepository
                        .findByIdAndElderProfileId(fid, eid)
                        .orElseThrow(() -> new ApiException(4042, "病情文件夹不存在"));
                d.setFolderId(fid);
            }
        }
        rebuildDerivedFields(d);
        d = documentRepository.save(d);
        return toDetail(d);
    }

    public void deleteDocument(AuthPrincipal principal, Long elderProfileId, long documentId) {
        long eid = medicalAccessService.resolveTargetElderProfileId(principal, elderProfileId);
        MedicalDocument d =
                documentRepository
                        .findByIdAndElderProfileId(documentId, eid)
                        .orElseThrow(() -> new ApiException(4041, "单据不存在"));

        var linkedEvents =
                medicalCalendarEventRepository.findByElderProfileIdAndSourceDocumentId(eid, d.getId());
        if (!linkedEvents.isEmpty()) {
            linkedEvents.forEach(event -> event.setSourceDocumentId(null));
            medicalCalendarEventRepository.saveAll(linkedEvents);
        }

        documentRepository.delete(d);

        try {
            medicalDocumentStorageService.deleteIfExists(d.getStoredPath());
        } catch (IOException ex) {
            log.warn(
                    "medical.archive.delete.file_cleanup_failed docId={} storedPath={} message={}",
                    d.getId(),
                    d.getStoredPath(),
                    ex.getMessage());
        }
    }

    public record FilePayload(Resource resource, MediaType mediaType) {}

    public FilePayload loadDocumentFile(AuthPrincipal principal, Long elderProfileId, long documentId)
            throws IOException {
        long eid = medicalAccessService.resolveTargetElderProfileId(principal, elderProfileId);
        MedicalDocument d =
                documentRepository
                        .findByIdAndElderProfileId(documentId, eid)
                        .orElseThrow(() -> new ApiException(4041, "单据不存在"));
        var path = medicalDocumentStorageService.resolveStoredFile(d.getStoredPath());
        byte[] bytes = Files.readAllBytes(path);
        MediaType mt = MediaType.APPLICATION_OCTET_STREAM;
        if (d.getContentType() != null && !d.getContentType().isBlank()) {
            try {
                mt = MediaType.parseMediaType(d.getContentType());
            } catch (Exception ignored) {
                mt = MediaType.APPLICATION_OCTET_STREAM;
            }
        }
        return new FilePayload(new ByteArrayResource(bytes), mt);
    }

    private MedicalDocumentDetailDto toDetail(MedicalDocument d) {
        ExtractedMedicalFieldsDto extracted = readExtracted(d);
        MedicalOcrView ocr = buildOcr(d);
        List<MedicalDisplayBlockDto> displayBlocks = readDisplayBlocks(d);
        Map<String, String> structuredFields = readStructuredFields(d);
        String structuredError = d.getStructuredError();

        if ((displayBlocks == null || displayBlocks.isEmpty())
                || (structuredFields == null || structuredFields.isEmpty())
                || (structuredError == null && ocr.getRoutedSpecializedApi() != null)) {
            var rendered =
                    medicalDocumentRenderService.renderFromStored(
                            d,
                            ocr,
                            extracted,
                            displayBlocks,
                            structuredFields,
                            structuredError);
            displayBlocks = rendered.displayBlocks();
            structuredFields = rendered.structuredFields();
            structuredError = rendered.structuredError();
        }

        ocr =
                MedicalOcrView.builder()
                        .fullText(ocr.getFullText())
                        .raw(ocr.getRaw())
                        .documentClasses(ocr.getDocumentClasses())
                        .classifyRaw(ocr.getClassifyRaw())
                        .routedSpecializedApi(ocr.getRoutedSpecializedApi())
                        .specializedRaw(ocr.getSpecializedRaw())
                        .structuredError(structuredError)
                        .classificationWarning(ocr.getClassificationWarning())
                        .structuredRouteSource(ocr.getStructuredRouteSource())
                        .build();

        return MedicalDocumentDetailDto.builder()
                .id(d.getId())
                .elderProfileId(d.getElderProfileId())
                .folderId(d.getFolderId())
            .folderName(resolveFolderName(d.getElderProfileId(), d.getFolderId()))
                .title(d.getTitle())
                .originalFilename(d.getOriginalFilename())
                .docCategory(d.getDocCategory())
                .routedSpecializedApi(d.getRoutedSpecializedApi())
                .structuredRouteSource(d.getStructuredRouteSource())
            .structuredError(structuredError)
                .fullText(d.getFullText())
                .extractedFields(extracted)
                .ocr(ocr)
                .displayBlocks(displayBlocks)
                .structuredFields(structuredFields)
                .createdAt(d.getCreatedAt())
                .build();
    }

    private void rebuildDerivedFields(MedicalDocument d) {
        MedicalOcrView ocr = buildOcr(d);
        ExtractedMedicalFieldsDto extracted = buildExtracted(d, ocr, true);
        var rendered = medicalDocumentRenderService.render(d, ocr, extracted);
        d.setDisplayBlocksJson(writeJsonSafe(rendered.displayBlocks()));
        d.setStructuredFieldsJson(writeJsonSafe(rendered.structuredFields()));
        d.setStructuredError(rendered.structuredError());
        d.setExtractedFieldsJson(writeJsonSafe(extracted));
    }

    private MedicalOcrView buildOcr(MedicalDocument d) {
        return MedicalOcrView.builder()
                .fullText(d.getFullText())
                .raw(readJsonMap(d.getOcrRawJson()))
                .documentClasses(null)
                .classifyRaw(readJsonMap(d.getClassifyRawJson()))
                .routedSpecializedApi(d.getRoutedSpecializedApi())
                .specializedRaw(readJsonMap(d.getSpecializedRawJson()))
                .structuredError(d.getStructuredError())
                .structuredRouteSource(d.getStructuredRouteSource())
                .build();
    }

    private ExtractedMedicalFieldsDto buildExtracted(MedicalDocument d, MedicalOcrView ocr) {
        return buildExtracted(d, ocr, false);
    }

    private ExtractedMedicalFieldsDto buildExtracted(
            MedicalDocument d, MedicalOcrView ocr, boolean forceRebuild) {
        if (!forceRebuild) {
            ExtractedMedicalFieldsDto extracted = readExtracted(d);
            if (extracted != null) {
                return extracted;
            }
        }
        var processed = medicalFieldPostProcessor.process(ocr.getFullText());
        MedicalOcrView cleaned =
                MedicalOcrView.builder()
                        .fullText(processed.getCleanedText())
                        .routedSpecializedApi(ocr.getRoutedSpecializedApi())
                        .build();
        return medicalFieldExtractionService.extract(cleaned);
    }

    private ExtractedMedicalFieldsDto readExtracted(MedicalDocument d) {
        if (d.getExtractedFieldsJson() == null || d.getExtractedFieldsJson().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(d.getExtractedFieldsJson(), ExtractedMedicalFieldsDto.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<MedicalDisplayBlockDto> readDisplayBlocks(MedicalDocument d) {
        if (d.getDisplayBlocksJson() == null || d.getDisplayBlocksJson().isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(
                    d.getDisplayBlocksJson(), new TypeReference<List<MedicalDisplayBlockDto>>() {});
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private Map<String, String> readStructuredFields(MedicalDocument d) {
        if (d.getStructuredFieldsJson() == null || d.getStructuredFieldsJson().isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(
                    d.getStructuredFieldsJson(), new TypeReference<Map<String, String>>() {});
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private Map<String, Object> readJsonMap(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
            return null;
        }
    }

    private String writeJsonSafe(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private MedicalDocumentSummaryDto toSummary(MedicalDocument d, Map<Long, String> folderNameById) {
        return MedicalDocumentSummaryDto.builder()
                .id(d.getId())
                .elderProfileId(d.getElderProfileId())
                .title(d.getTitle())
                .docCategory(d.getDocCategory())
                .routedSpecializedApi(d.getRoutedSpecializedApi())
                .folderId(d.getFolderId())
                .folderName(d.getFolderId() == null ? null : folderNameById.get(d.getFolderId()))
                .createdAt(d.getCreatedAt())
                .build();
    }

    private Map<Long, String> loadFolderNameById(Long elderProfileId) {
        Map<Long, String> folderNameById = new HashMap<>();
        for (MedicalArchiveFolder folder : folderRepository.findByElderProfileIdOrderBySortOrderAscIdAsc(elderProfileId)) {
            folderNameById.put(folder.getId(), folder.getName());
        }
        return folderNameById;
    }

    private String resolveFolderName(Long elderProfileId, Long folderId) {
        if (folderId == null) {
            return null;
        }
        return folderRepository
                .findByIdAndElderProfileId(folderId, elderProfileId)
                .map(MedicalArchiveFolder::getName)
                .orElse(null);
    }

    private static MedicalArchiveFolderViewDto toFolderView(MedicalArchiveFolder f) {
        return MedicalArchiveFolderViewDto.builder()
                .id(f.getId())
                .elderProfileId(f.getElderProfileId())
                .name(f.getName())
                .sortOrder(f.getSortOrder())
                .createdAt(f.getCreatedAt())
                .build();
    }
}
