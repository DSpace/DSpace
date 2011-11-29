  -- Cración del grupo SeDiCIAdmin para tareas las tareas del workflow
  -- Está pensado para ser ejecutado durante la instalación. En este punto 
  -- solo existen el grupo 0:Anonymous y el 1:Adminitrator.
  -- Agregamos el grupo 2:SeDiCIAdmin para el funcionamiento del xmlworkflow 
INSERT INTO epersongroup (eperson_group_id, name) VALUES (2, 'SeDiCIAdmin');

-- Actualización de la secuencia
SELECT setval('epersongroup_seq', max(eperson_group_id)) FROM epersongroup;