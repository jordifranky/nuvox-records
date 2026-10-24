-- Esquema de Nuvox Records sin usuarios ni demos de ejemplo.
-- Ejecutar solo en una base vacía; para transferir datos reales usar un backup de PostgreSQL.

--
-- PostgreSQL database dump
--

\restrict PWJYRk5JOnY9CZssb67Bnuy7hnvERARK366sQQPISy64SKnnzF2rIaZF2nkM0wu

-- Dumped from database version 18.4
-- Dumped by pg_dump version 18.4

-- Started on 2026-10-15 21:41:12

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 222 (class 1259 OID 17287)
-- Name: demos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.demos (
                              id bigint NOT NULL,
                              codigo_expediente character varying(30) NOT NULL,
                              titulo_cancion character varying(150) NOT NULL,
                              nombre_artistico character varying(120) NOT NULL,
                              correo_artista character varying(120) NOT NULL,
                              genero_musical character varying(80) NOT NULL,
                              pais character varying(60) NOT NULL,
                              bpm integer,
                              tonalidad character varying(40),
                              formato_audio character varying(60) NOT NULL,
                              enlace_audio character varying(500) NOT NULL,
                              notas_artista text,
                              estado_curatorial character varying(30) DEFAULT 'EN_REVISION'::character varying NOT NULL,
                              dictamen_comite text,
                              fecha_envio timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
                              CONSTRAINT chk_estado_curatorial CHECK (((estado_curatorial)::text = ANY ((ARRAY['EN_REVISION'::character varying, 'APROBADO'::character varying, 'RECHAZADO'::character varying])::text[])))
);


--
-- TOC entry 221 (class 1259 OID 17286)
-- Name: demos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.demos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 5036 (class 0 OID 0)
-- Dependencies: 221
-- Name: demos_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.demos_id_seq OWNED BY public.demos.id;


--
-- TOC entry 220 (class 1259 OID 17266)
-- Name: usuarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.usuarios (
                                 id bigint NOT NULL,
                                 username character varying(120) NOT NULL,
                                 password character varying(255) NOT NULL,
                                 nombre_completo character varying(120) NOT NULL,
                                 rol character varying(50) DEFAULT 'ROLE_ARTISTA'::character varying NOT NULL,
                                 activo boolean DEFAULT true NOT NULL,
                                 CONSTRAINT chk_rol_usuario CHECK (((rol)::text = ANY ((ARRAY['ROLE_COMITE_AR'::character varying, 'ROLE_ARTISTA'::character varying, 'ROLE_ADMIN'::character varying])::text[])))
);


--
-- TOC entry 219 (class 1259 OID 17265)
-- Name: usuarios_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.usuarios_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 5037 (class 0 OID 0)
-- Dependencies: 219
-- Name: usuarios_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.usuarios_id_seq OWNED BY public.usuarios.id;


--
-- TOC entry 4864 (class 2604 OID 17290)
-- Name: demos id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.demos ALTER COLUMN id SET DEFAULT nextval('public.demos_id_seq'::regclass);


--
-- TOC entry 4861 (class 2604 OID 17269)
-- Name: usuarios id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios ALTER COLUMN id SET DEFAULT nextval('public.usuarios_id_seq'::regclass);


--
-- TOC entry 5030 (class 0 OID 17287)
-- Dependencies: 222
-- Data for Name: demos; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 5028 (class 0 OID 17266)
-- Dependencies: 220
-- Data for Name: usuarios; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- TOC entry 5038 (class 0 OID 0)
-- Dependencies: 221
-- Name: demos_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--



--
-- TOC entry 5039 (class 0 OID 0)
-- Dependencies: 219
-- Name: usuarios_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--



--
-- TOC entry 4875 (class 2606 OID 17310)
-- Name: demos demos_codigo_expediente_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.demos
    ADD CONSTRAINT demos_codigo_expediente_key UNIQUE (codigo_expediente);


--
-- TOC entry 4877 (class 2606 OID 17308)
-- Name: demos demos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.demos
    ADD CONSTRAINT demos_pkey PRIMARY KEY (id);


--
-- TOC entry 4871 (class 2606 OID 17282)
-- Name: usuarios usuarios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_pkey PRIMARY KEY (id);


--
-- TOC entry 4873 (class 2606 OID 17284)
-- Name: usuarios usuarios_username_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_username_key UNIQUE (username);


--
-- TOC entry 4878 (class 1259 OID 17311)
-- Name: idx_demos_codigo_expediente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_demos_codigo_expediente ON public.demos USING btree (codigo_expediente);


--
-- TOC entry 4879 (class 1259 OID 17312)
-- Name: idx_demos_estado_curatorial; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_demos_estado_curatorial ON public.demos USING btree (estado_curatorial);


--
-- TOC entry 4869 (class 1259 OID 17285)
-- Name: idx_usuarios_username; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_usuarios_username ON public.usuarios USING btree (username);


-- Completed on 2026-10-15 21:41:12

--
-- PostgreSQL database dump complete
--

\unrestrict PWJYRk5JOnY9CZssb67Bnuy7hnvERARK366sQQPISy64SKnnzF2rIaZF2nkM0wu

