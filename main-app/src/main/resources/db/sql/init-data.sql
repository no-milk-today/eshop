-- Disable foreign key checks to avoid issues during truncation
SET FOREIGN_KEY_CHECKS = 0;

-- Truncate dependent tables first to satisfy foreign key constraints
TRUNCATE TABLE order_products;
TRUNCATE TABLE orders;
TRUNCATE TABLE cart_products;
TRUNCATE TABLE carts;
TRUNCATE TABLE products;
TRUNCATE TABLE users;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- Insert default user with id 1
INSERT INTO users (id, username, password, email)
VALUES (1, 'default', 'pass', 'default@example.com'),
       (2, 'alice', 'alicepass', 'alice@example.com'),
       (3, 'bob', 'bobpass', 'bob@example.com');

-- Inserts 15 test products for ProductController testing
INSERT INTO products (id, name, price, description, img_path)
VALUES (1, 'Кепка', 10.00, 'Description for Product 1', 'https://www.club100.kz/imgs/bbcf/3.4.jpg.webp?t=25032721'),
       (2, 'Рубаха', 20.00, 'Description for Product 2', 'https://folkberry.ru/image/cache/catalog/products/rubahi/muzhskaya-rubaha-istoki/beliy-s-zelenim-1-396x396-product_popup.jpg'),
       (3, 'Брюки М', 30.00, 'Description for Product 3', 'https://elema.by/upload/iblock/612/szpqgvtygjr42f9aynq0yhe2zkkl8x1l/Bryuki-zhenskie-3K_103_1-chyernyy-_1_.jpg'),
       (4, 'Брюки Ж', 40.00, 'Description for Product 4', 'https://barmariska.ru/wp-content/uploads/2022/09/G-G0570-zGa-l-5.jpg?x79562'),
       (5, 'Джемпер 1', 50.00, 'Description for Product 5', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ14jNFuk15h10SDSS2ljiC9SnGBphtkAA2YA&s'),
       (6, 'Джемпер 2', 60.00, 'Description for Product 6', 'https://elema.by/upload/iblock/c7c/b6i7ipeu4jw0m30mae1wndu9yq6o61bw/Dzhemper-zhenskiy-2K_12771_1-bezhevyy-_3_.jpg'),
       (7, 'Кеды 1', 70.00, 'Description for Product 7', 'https://www.converseforminsk.by/wp-content/uploads/2021/01/M9160C_new-1.jpg'),
       (8, 'Кеды 2', 80.00, 'Description for Product 8', 'https://www.ecco-shoes.by/images/eshop/img/jpg/bigw/291143_01001.jpg?v=3'),
       (9, 'Кроссовки 1', 90.00, 'Description for Product 9', 'https://ir-3.ozone.ru/s3/multimedia-1-h/c1000/6998013881.jpg'),
       (10, 'Кроссовки 2', 100.00, 'Description for Product 10', 'https://lauf.shoes/upload/iblock/c17/a6kiw1p2xiq4twmfr3wsropsq0v6z8oj/1Y8A2145_web.jpg'),
       (11, 'Свитер Ж', 110.00, 'Description for Product 11', 'https://imageproxy.fh.by/qLscxe0f8k0W9_Py1WBuSD3tW8sfn7WATzNt7EMb8JU/w:894/h:1342/rt:fit/q:95/czM6Ly9maC1wcm9kdWN0aW9uLXJmMy84MzEwMTcvNDZhMTUwZjYtNjc0Mi0xMWVlLThiOTQtMDA1MDU2ODM2NjFi.jpg'),
       (12, 'Свитер М', 120.00, 'Description for Product 12', 'https://cdn.finnflare.com/upload/resize_cache/full_size/FWC/180/836_1214_2/FWC11138_180_100.webp?cdn=1695643794'),
       (13, 'Топик 1', 130.00, 'Description for Product 13', 'https://basket-11.wbbasket.ru/vol1635/part163579/163579392/images/big/1.webp'),
       (14, 'Топик 2', 140.00, 'Description for Product 14', 'https://zaragoza.com.ua/content/images/28/356x356l85nn0/40906784062082.jpg'),
       (15, 'Шапка 1', 150.00, 'Description for Product 15', 'https://ligatura.by/image/catalog/products/47110_1.jpg');