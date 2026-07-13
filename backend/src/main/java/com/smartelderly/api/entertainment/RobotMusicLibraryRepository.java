package com.smartelderly.api.entertainment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RobotMusicLibraryRepository extends JpaRepository<RobotMusicLibrary, Long> {

    List<RobotMusicLibrary> findByEnabledTrueOrderByIdAsc();
}
