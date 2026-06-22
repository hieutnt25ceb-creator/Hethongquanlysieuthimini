USE minimart_db;

UPDATE users SET password = '$2a$12$V0DbYqNhh94vEsbTLmbZ4Onz84rd4BNkcVQaqkPuWvR521sYiad3e' WHERE username = 'admin';
UPDATE users SET password = '$2a$12$A2hG0kecyxExMyapgxgMwenXQM8bz5r//SsQ8FCTRssmHg3fsFMLi' WHERE username IN ('nhanvien1', 'nhanvien2');
