ALTER TABLE products
    ADD COLUMN book_author VARCHAR(255) NOT NULL DEFAULT 'Unknown',
    ADD COLUMN isbn VARCHAR(20) NULL,
    ADD COLUMN publisher VARCHAR(255) NULL,
    ADD COLUMN publication_year INT NULL,
    ADD COLUMN book_language VARCHAR(80) NULL,
    ADD COLUMN page_count INT NULL,
    ADD COLUMN book_format VARCHAR(50) NOT NULL DEFAULT 'PAPERBACK',
    ADD COLUMN dimensions VARCHAR(120) NULL;

CREATE UNIQUE INDEX uk_products_isbn ON products (isbn);
CREATE INDEX idx_products_book_author ON products (book_author);
CREATE INDEX idx_products_publisher ON products (publisher);
CREATE INDEX idx_products_publication_year ON products (publication_year);
