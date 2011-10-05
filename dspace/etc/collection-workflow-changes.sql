CREATE SEQUENCE collectionrole_seq INCREMENT BY 1 NO MAXVALUE NO MINVALUE CACHE 1;

ALTER TABLE public.collectionrole_seq OWNER TO dryad_app;


CREATE TABLE collectionrole (
collectionrole_id integer DEFAULT nextval('collectionrole_seq'::regclass) NOT NULL,
role_id Text,
collection_id integer REFERENCES collection(collection_id),
group_id integer REFERENCES epersongroup(eperson_group_id)
);

ALTER TABLE ONLY collectionrole
ADD CONSTRAINT collectionrole_pkey PRIMARY KEY (collectionrole_id);
ALTER TABLE ONLY collectionrole
ADD CONSTRAINT collectionrole_unique UNIQUE (role_id, collection_id, group_id);

ALTER TABLE public.collectionrole OWNER TO dryad_app;


CREATE SEQUENCE workflowitemrole_seq INCREMENT BY 1 NO MAXVALUE NO MINVALUE CACHE 1;

ALTER TABLE public.workflowitemrole_seq OWNER TO dryad_app;


CREATE TABLE workflowitemrole (
workflowitemrole_id integer DEFAULT nextval('workflowitemrole_seq'::regclass) NOT NULL,
role_id Text,
workflow_item_id integer REFERENCES workflowitem(workflow_id),
eperson_id integer,
group_id integer REFERENCES epersongroup(eperson_group_id)
);


-- TODO: this should normally work, but causes issues on karya
-- ALTER TABLE ONLY workflowitemrole
--     ADD CONSTRAINT workflowitemrole_eperson_id_fkey FOREIGN KEY (eperson_id) REFERENCES eperson(eperson_id);

ALTER TABLE ONLY workflowitemrole
ADD CONSTRAINT workflowitemrole_pkey PRIMARY KEY (workflowitemrole_id);
ALTER TABLE ONLY workflowitemrole
ADD CONSTRAINT workflowitemrole_unique UNIQUE (role_id, workflow_item_id, eperson_id);


ALTER TABLE public.workflowitemrole OWNER TO dryad_app;

-- CREATE SEQUENCE workflowassignment_seq INCREMENT BY 1 NO MAXVALUE NO MINVALUE CACHE 1;
--
-- CREATE TABLE WorkflowAssignment (
-- workflow_assignment_id integer DEFAULT nextval('workflowassignment_seq'::regclass) NOT NULL,
-- role_id Text,
-- group_id integer REFERENCES epersongroup(eperson_group_id),
-- collection_id integer REFERENCES collection(collection_id));
--
-- ALTER TABLE ONLY WorkflowAssignment
-- ADD CONSTRAINT workflow_assignment_pkey PRIMARY KEY (workflow_assignment_id);
-- ALTER TABLE ONLY WorkflowAssignment
-- ADD CONSTRAINT workflow_assignment_unique UNIQUE (role_id, collection_id);

ALTER TABLE tasklistitem ADD step_id text;
ALTER TABLE tasklistitem ADD action_id text;
-- TODO: Please take into account that the workflow_id column type has been changed and tis may be important for backwards comp
--TODO: REPLACE workflowitem_id by workflow_item_id
ALTER TABLE tasklistitem ADD workflow_item_id integer;
ALTER TABLE tasklistitem DROP workflow_id;
ALTER TABLE tasklistitem ADD workflow_id Text;

CREATE SEQUENCE taskowner_seq INCREMENT BY 1 NO MAXVALUE NO MINVALUE CACHE 1;

ALTER TABLE public.taskowner_seq OWNER TO dryad_app;


-- TODO: restore reference to eperson from owner_id !
CREATE TABLE taskowner (
taskowner_id integer DEFAULT nextval('taskowner_seq'::regclass) NOT NULL,
workflow_item_id integer REFERENCES workflowitem(workflow_id),
step_id Text,
action_id Text,
workflow_id Text,
owner_id integer);

ALTER TABLE ONLY taskowner
ADD CONSTRAINT taskowner_pkey PRIMARY KEY (taskowner_id);
ALTER TABLE ONLY taskowner
ADD CONSTRAINT taskowner_unique UNIQUE (step_id, workflow_item_id, workflow_id, owner_id, action_id);


ALTER TABLE public.taskowner OWNER TO dryad_app;
