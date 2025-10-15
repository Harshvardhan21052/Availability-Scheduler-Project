package com.scheduler.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.scheduler.entity.BusySlot;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class BusySlotResponse {

    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    public static BusySlotResponse from(BusySlot slot) {
        BusySlotResponse res = new BusySlotResponse();
        res.setId(slot.getId());
        res.setDate(slot.getDate());
        res.setStartTime(slot.getStartTime());
        res.setEndTime(slot.getEndTime());
        return res;
    }
}
