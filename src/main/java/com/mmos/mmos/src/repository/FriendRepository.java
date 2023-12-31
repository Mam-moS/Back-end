package com.mmos.mmos.src.repository;

import com.mmos.mmos.src.domain.entity.Friend;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, Long> {

    Optional<Friend> findFriendByUser_UserIndexAndFriendUserIndex(Long userIdx, Long friendIdx);

    List<Friend> findFriendsByUser_UserIndexAndFriendStatus(Long userIdx, Integer status);
}
