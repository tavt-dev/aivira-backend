UPDATE products p
LEFT JOIN (
    SELECT oi.product_id, SUM(oi.quantity) AS quantity_sold
    FROM order_items oi
    INNER JOIN orders o ON o.id = oi.order_id
    WHERE o.order_status = 'COMPLETED'
    GROUP BY oi.product_id
) completed_sales ON completed_sales.product_id = p.id
SET p.sold_count = COALESCE(completed_sales.quantity_sold, 0);
