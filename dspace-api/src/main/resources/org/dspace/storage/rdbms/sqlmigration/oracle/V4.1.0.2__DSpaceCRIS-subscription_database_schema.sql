ALTER TABLE subscription ADD community_id INTEGER;
ALTER TABLE subscription ADD CONSTRAINT subscription_community_id FOREIGN KEY (community_id) REFERENCES community;