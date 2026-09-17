package com.ridesync.dto;

import lombok.Data;

@Data
public class LocationUpdateRequest {
    private Double latitude;
    private Double longitude;
    private boolean available;
}
