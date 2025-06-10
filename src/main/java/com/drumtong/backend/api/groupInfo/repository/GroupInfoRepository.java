package com.drumtong.backend.api.groupInfo.repository;

import com.drumtong.backend.api.groupInfo.entity.GroupInfo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface GroupInfoRepository extends JpaRepository<GroupInfo, Long> {
    Optional<GroupInfo> findByName(@Param("name") String name);
}
