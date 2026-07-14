package com.cpesu.cpecraft.db;

import java.util.UUID;

public record StudentRecord(
		UUID uuid,
		String username,
		String studentId,
		String name,
        String nickName,
		String batch,
		long verifiedAt) {
}
