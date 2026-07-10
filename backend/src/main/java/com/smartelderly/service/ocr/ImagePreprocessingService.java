package com.smartelderly.service.ocr;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Iterator;
import java.util.UUID;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.smartelderly.config.AppProperties;

@Service
public class ImagePreprocessingService {

	private static final Logger log = LoggerFactory.getLogger(ImagePreprocessingService.class);

	private final AppProperties appProperties;

	public ImagePreprocessingService(AppProperties appProperties) {
		this.appProperties = appProperties;
	}

	public byte[] preprocess(byte[] inputBytes) {
		if (inputBytes == null || inputBytes.length == 0) {
			return inputBytes;
		}

		AppProperties.Preprocess cfg = appProperties.getPreprocess();
		if (cfg == null || !cfg.isEnabled()) {
			return inputBytes;
		}

		try {
			BufferedImage source = ImageIO.read(new ByteArrayInputStream(inputBytes));
			if (source == null) {
				log.info("ocr.preprocess.skip reason=unsupported-image inputBytes={}", inputBytes.length);
				return inputBytes;
			}

			BufferedImage working = source;
			int originalWidth = working.getWidth();
			int originalHeight = working.getHeight();

			int maxWidth = cfg.getMaxWidth();
			if (maxWidth > 0 && working.getWidth() > maxWidth) {
				double scale = (double) maxWidth / working.getWidth();
				int resizedWidth = maxWidth;
				int resizedHeight = Math.max(1, (int) Math.round(working.getHeight() * scale));
				BufferedImage resized = new BufferedImage(resizedWidth, resizedHeight, BufferedImage.TYPE_INT_RGB);
				Graphics2D g = resized.createGraphics();
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g.drawImage(working, 0, 0, resizedWidth, resizedHeight, null);
				g.dispose();
				working = resized;
			}

			BufferedImage gray = new BufferedImage(working.getWidth(), working.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
			Graphics2D g2 = gray.createGraphics();
			g2.drawImage(working, 0, 0, null);
			g2.dispose();

			byte[] output = encodeJpeg(gray, cfg.getJpegQuality());
			saveDebugCopy(cfg.getTempDir(), output);

			log.info(
					"ocr.preprocess.done enabled={} inputBytes={} outputBytes={} originalWxH={}x{} outputWxH={}x{}",
					cfg.isEnabled(),
					inputBytes.length,
					output.length,
					originalWidth,
					originalHeight,
					gray.getWidth(),
					gray.getHeight());
			return output;
		} catch (Exception e) {
			log.warn("ocr.preprocess.failed inputBytes={} reason={}", inputBytes.length, e.getMessage());
			return inputBytes;
		}
	}

	private static byte[] encodeJpeg(BufferedImage image, float quality) throws IOException {
		float clampedQuality = Math.max(0.05f, Math.min(1.0f, quality));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
		ImageWriter writer = writers.hasNext() ? writers.next() : null;
		if (writer == null) {
			ImageIO.write(image, "jpg", baos);
			return baos.toByteArray();
		}

		ImageWriteParam param = writer.getDefaultWriteParam();
		param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		param.setCompressionQuality(clampedQuality);
		try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
			writer.setOutput(ios);
			writer.write(null, new IIOImage(image, null, null), param);
			writer.dispose();
		}
		return baos.toByteArray();
	}

	private static void saveDebugCopy(String tempDir, byte[] output) {
		if (tempDir == null || tempDir.isBlank() || output == null || output.length == 0) {
			return;
		}
		try {
			Path dir = Path.of(tempDir);
			Files.createDirectories(dir);
			String name = "ocr-preprocess-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + ".jpg";
			Path target = dir.resolve(name);
			Files.write(target, output);
			log.info("ocr.preprocess.saved path={}", target.toAbsolutePath());
		} catch (Exception e) {
			log.warn("ocr.preprocess.save_failed reason={}", e.getMessage());
		}
	}
}
