SET search_path = pg_catalog;

CREATE RULE on_delete AS
    ON DELETE TO public.t1 DO  DELETE FROM public.user_data WHERE (user_data.id = old.c1);

CREATE RULE on_insert AS
    ON INSERT TO public.t1 DO INSTEAD ( INSERT INTO public.user_data (id, email, created) VALUES (new.c1, new.c2, new.c3);
 INSERT INTO public.t1 DEFAULT VALUES;
);

CREATE RULE on_update AS
    ON UPDATE TO public.t1 DO INSTEAD  UPDATE public.user_data SET id = new.c1, email = new.c2, created = new.c3 WHERE (user_data.id = old.c1);

CREATE RULE on_update_nothing AS
    ON UPDATE TO public.t1
  WHERE (1=1) DO INSTEAD NOTHING;