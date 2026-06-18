ALTER TABLE sy_platform_setting
  ADD COLUMN browser_title VARCHAR(100),
  ADD COLUMN theme_color VARCHAR(20),
  ADD COLUMN favicon_url TEXT,
  ADD COLUMN top_logo_url TEXT,
  ADD COLUMN top_logo_small_url TEXT,
  ADD COLUMN login_main_image_url TEXT;
