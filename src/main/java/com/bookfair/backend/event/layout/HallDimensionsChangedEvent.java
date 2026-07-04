package com.bookfair.backend.event.layout;

import java.util.UUID;

public record HallDimensionsChangedEvent(UUID hallId, Integer newWidth, Integer newHeight) {
}
