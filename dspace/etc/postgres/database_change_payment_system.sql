-- transactions:
-- - id: Id of CC transaction, voucher or subscription used
-- - expiration: final date that transaction will be valid until.
-- - type: credit card, voucher, or subscription
-- - status: open|completed|rejected|...
-- - depositor: submitter that used it
-- - item: DSpace Item it was used on
-- - currency: currency selected during the submission process
-- - country: country selected during the submission process
-- - voucher code: added during the submission process

CREATE SEQUENCE shoppingcart_seq;

CREATE TABLE shoppingcart
(
  cart_id INTEGER PRIMARY KEY,
  expiration date,
  status VARCHAR(256),
  depositor INTEGER,
  item INTEGER,
  currency VARCHAR(256),
  country VARCHAR(256),
  voucher VARCHAR(256),
  total DOUBLE PRECISION,
  transaction_id VARCHAR(256),
  securetoken VARCHAR(256)
);

