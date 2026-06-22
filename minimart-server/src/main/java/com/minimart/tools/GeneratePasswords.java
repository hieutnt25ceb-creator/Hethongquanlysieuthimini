package com.minimart.tools;

import com.minimart.server.security.BCryptUtil;

/**
 * Tiện ích tạo và kiểm tra BCrypt hash.
 */
public class GeneratePasswords {
    public static void main(String[] args) {
        String adminHash = "$2a$12$8/RzXfNgDH0Ff8lmgHJVF.R0EYZNm8b1bV5pHTSFzuJdF1nO7d2tK";
        String empHash = "$2a$12$pC1Bq.d/RtYXn7Hq8lM2QuzgZP3q1.rSw0LUuQNKfxc.JZD6Yk8i6";

        System.out.println("Verify Admin@123 with adminHash: " + BCryptUtil.verifyPassword("Admin@123", adminHash));
        System.out.println("Verify Emp@123 with empHash: " + BCryptUtil.verifyPassword("Emp@123", empHash));
        System.out.println("Verify admin123 with adminHash: " + BCryptUtil.verifyPassword("admin123", adminHash));
        System.out.println("Verify emp123 with empHash: " + BCryptUtil.verifyPassword("emp123", empHash));
    }
}
