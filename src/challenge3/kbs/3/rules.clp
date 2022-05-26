
;; 

( defrule iphone13-banamex-rule
    (order (order-number ?on) (pay-type card) (customer-id ?cid) (finish no))
    (card (order-number ?on) (type credit) (bank Banamex))
    (line-item (order-number ?on) (product-id 2) (quantity ?q))
    (product (id 2) (price ?p) (amount ?a))
    (test (> ?q 0))
    (test (> ?a 0))
    => 
    (assert (result (customer-id ?cid) (offer 25M) (description "25 meses sin intereses") (product-id 2) (price ?p) (order-id ?on) (amount ?a)))
)


;;

( defrule iphone13-american-rule
    (order (order-number ?on) (pay-type card) (customer-id ?cid) (finish no))
    (card (order-number ?on) (type credit) (bank AmericanExpress))
    (line-item (order-number ?on) (product-id 2) (quantity ?q))
    (product (id 2) (price ?p) (amount ?a))
    (test (> ?q 0))
    (test (> ?a 0))
    => 
    (assert (result (customer-id ?cid) (offer 25M) (description "25 meses sin intereses") (product-id 2) (price ?p) (order-id ?on) (amount ?a)))
)


;; 

( defrule note-12-visa-rule 
    (order (order-number ?on) (pay-type card) (customer-id ?cid) (finish no))
    (card (order-number ?on) (type credit) (bank "Liverpool Visa")) 
    (line-item (order-number ?on) (product-id 4) (quantity ?q))
    (product (id 4) (price ?p) (amount ?a))
    (test (> ?q 0))
    (test (> ?a 0))
    =>
    (assert (result (customer-id ?cid) (offer 11M) (description "11 meses sin intereses") (product-id 4) (price ?p) (order-id ?on) (amount ?a)))
)


;; 

( defrule macbook-iphone-rule
    (order (order-number ?on) (pay-type cash) (customer-id ?cid) (finish no))
    (line-item (order-number ?on) (product-id 3) (quantity ?qm))
    (line-item (order-number ?on) (product-id 1) (quantity ?qi))
    (product (id 3) (price ?p1) (amount ?am))
    (product (id 1) (price ?p2) (amount ?ai))
    (test (> ?qm 0))
    (test (> ?qi 0))
    (test (> ?am 0))
    (test (> ?ai 0))
    =>
    (assert (result (customer-id ?cid) (offer 90vx1200c) (description "90 pesos en vales x cada 1200 pesos de compra") (product-id 1) (price ?p2) (order-id ?on) (amount ?ai)))
    (assert (result (customer-id ?cid) (offer 90vx1200c) (description "90 pesos en vales x cada 1200 pesos de compra") (product-id 3) (price ?p1) (order-id ?on) (amount ?am)))
)


;;

( defrule smartphone-f-m-rule
    (order (order-number ?on) (pay-type ?pt) (customer-id ?cid) (finish no))
    (line-item (order-number ?on) (product-id ?pid) (quantity ?q))
    (product (id ?pid) (category smartphone) (price ?p) (amount ?a))
    (test (> ?q 0))
    (test (> ?a 0))
    => 
    (assert (result (customer-id ?cid) (offer FyM10%) (description "Funda y mica con 10% de descuento") (product-id ?pid) (price ?p) (order-id ?on) (amount 1)))
)