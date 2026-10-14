-- 基础分类；保留现有分类和所有业务数据。
INSERT INTO categories (name)
VALUES ('数码电子'), ('图书教材'), ('服饰鞋包'), ('生活用品'), ('运动户外'), ('其他')
ON CONFLICT DO NOTHING;
