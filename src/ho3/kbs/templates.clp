;; Products template

( deftemplate product
    (slot id)
    (slot category)
    (slot marca)
    (slot model)
    (slot price)
    (slot amount)
)


;; Card template 

( deftemplate card 
    (slot order-number)
    (slot type)
    (slot bank)
)


;; Order template

( deftemplate order 
    (slot order-number)
    (slot customer-id)
    (slot pay-type)
    (slot finish (type SYMBOL) (default no) (allowed-symbols yes no))
)


;; Line-item template

( deftemplate line-item
    (slot order-number)
    (slot product-id)
    (slot quantity (default 1))
)


;; Customer template 


( deftemplate customer 
    (slot id)
    (multislot name)
    (multislot address)
    (slot phone)
)


;; Result template 

( deftemplate result 
    (slot customer-id)
    (slot offer)
    (slot description)
    (slot price)
    (slot product-id)
    (slot order-id)
    (slot amount)
)