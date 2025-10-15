package com.scheduler.repository;

import com.scheduler.entity.BusySlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BusySlotRepository extends JpaRepository<BusySlot, Long> {

    List<BusySlot> findByUserIdOrderByDateAscStartTimeAsc(Long userId);

    List<BusySlot> findByUserIdAndDateOrderByStartTimeAsc(Long userId, LocalDate date);
}
