package com.drumtong.backend.api.group.repository;

import com.drumtong.backend.api.group.entity.GroupInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupInfoRepository extends JpaRepository<GroupInfo, Long> {
}
