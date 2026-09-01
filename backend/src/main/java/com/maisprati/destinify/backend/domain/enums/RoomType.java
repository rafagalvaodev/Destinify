package com.maisprati.destinify.backend.domain.enums;

public enum RoomType {
    SINGLE(1),
    SUITE(2),
    TRIPLE(3);

    private  final int maxCapacity;

    RoomType(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }
}
