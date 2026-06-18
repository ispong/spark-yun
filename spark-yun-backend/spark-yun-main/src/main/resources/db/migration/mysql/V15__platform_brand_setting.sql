ALTER TABLE sy_platform_setting
  ADD COLUMN browser_title VARCHAR(100),
  ADD COLUMN theme_color VARCHAR(20),
  ADD COLUMN favicon_url LONGTEXT,
  ADD COLUMN top_logo_url LONGTEXT,
  ADD COLUMN top_logo_small_url LONGTEXT,
  ADD COLUMN login_main_image_url LONGTEXT;
