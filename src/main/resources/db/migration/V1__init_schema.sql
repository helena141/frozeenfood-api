--
-- PostgreSQL database dump
--

\restrict fMGEu86FbWTvZPf1lwFn02FZMuNSPPSJMaWd7lDfg0x3d4M7bcEd0whUYyKGuco

-- Dumped from database version 18.6
-- Dumped by pg_dump version 18.6

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
-- Name: customers; Type: TABLE; Schema: public; Owner: admin_b2b
--

CREATE TABLE public.customers (
    id_user uuid NOT NULL,
    alamat text NOT NULL,
    created_at timestamp(6) without time zone,
    deleted_at timestamp(6) without time zone,
    email character varying(150) NOT NULL,
    is_active boolean NOT NULL,
    nama character varying(150) NOT NULL,
    no_telp character varying(25) NOT NULL,
    tipe_akun character varying(20) NOT NULL,
    updated_at timestamp(6) without time zone
);


ALTER TABLE public.customers OWNER TO admin_b2b;

--
-- Name: deliveries; Type: TABLE; Schema: public; Owner: admin_b2b
--

CREATE TABLE public.deliveries (
    id_delivery uuid NOT NULL,
    alamat_tujuan character varying(255),
    created_at timestamp(6) without time zone,
    deleted_at timestamp(6) without time zone,
    jarak real,
    metode_deliv character varying(255),
    status_delivery character varying(255),
    updated_at timestamp(6) without time zone,
    id_order uuid
);


ALTER TABLE public.deliveries OWNER TO admin_b2b;

--
-- Name: expiry_alerts; Type: TABLE; Schema: public; Owner: admin_b2b
--

CREATE TABLE public.expiry_alerts (
    id_alert uuid NOT NULL,
    created_at timestamp(6) without time zone,
    deleted_at timestamp(6) without time zone,
    hari_sebelum integer,
    is_sent boolean,
    send_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id_prod_batch uuid
);


ALTER TABLE public.expiry_alerts OWNER TO admin_b2b;

--
-- Name: order_items; Type: TABLE; Schema: public; Owner: admin_b2b
--

CREATE TABLE public.order_items (
    id_order_item uuid NOT NULL,
    harga_satuan numeric(15,2) NOT NULL,
    quantity integer NOT NULL,
    subtotal_item numeric(15,2) NOT NULL,
    id_order uuid NOT NULL,
    id_prod_batch uuid NOT NULL
);


ALTER TABLE public.order_items OWNER TO admin_b2b;

--
-- Name: orders; Type: TABLE; Schema: public; Owner: admin_b2b
--

CREATE TABLE public.orders (
    id_order uuid NOT NULL,
    catatan text,
    created_at timestamp(6) without time zone,
    deleted_at timestamp(6) without time zone,
    ongkir numeric(15,2) NOT NULL,
    status character varying(50) NOT NULL,
    tipe_order character varying(20) NOT NULL,
    total numeric(15,2) NOT NULL,
    updated_at timestamp(6) without time zone,
    id_user uuid NOT NULL
);


ALTER TABLE public.orders OWNER TO admin_b2b;

--
-- Name: payments; Type: TABLE; Schema: public; Owner: admin_b2b
--

CREATE TABLE public.payments (
    id_payment uuid NOT NULL,
    created_at timestamp(6) without time zone,
    deleted_at timestamp(6) without time zone,
    expired_at timestamp(6) without time zone,
    gateway_ref character varying(255),
    jumlah numeric(38,2),
    metode_pay character varying(255),
    paid_at timestamp(6) without time zone,
    status character varying(255),
    updated_at timestamp(6) without time zone,
    id_order uuid
);


ALTER TABLE public.payments OWNER TO admin_b2b;

--
-- Name: products; Type: TABLE; Schema: public; Owner: admin_b2b
--

CREATE TABLE public.products (
    id_produk uuid NOT NULL,
    berat numeric(10,2) NOT NULL,
    created_at timestamp(6) without time zone,
    deleted_at timestamp(6) without time zone,
    deskripsi_produk text,
    gambar text,
    harga_eceran numeric(15,2) NOT NULL,
    is_active boolean NOT NULL,
    kategori character varying(100) NOT NULL,
    nama_produk character varying(200) NOT NULL,
    updated_at timestamp(6) without time zone,
    volume numeric(10,2)
);


ALTER TABLE public.products OWNER TO admin_b2b;

--
-- Name: produk_batches; Type: TABLE; Schema: public; Owner: admin_b2b
--

CREATE TABLE public.produk_batches (
    id_prod_batch uuid NOT NULL,
    created_at timestamp(6) without time zone,
    deleted_at timestamp(6) without time zone,
    diskon numeric(5,2),
    harga_override numeric(15,2),
    stok_awal integer NOT NULL,
    stok_online integer NOT NULL,
    tanggal_exp date NOT NULL,
    updated_at timestamp(6) without time zone,
    id_produk uuid NOT NULL
);


ALTER TABLE public.produk_batches OWNER TO admin_b2b;

--
-- Name: tiering_prices; Type: TABLE; Schema: public; Owner: admin_b2b
--

CREATE TABLE public.tiering_prices (
    id_tiering uuid NOT NULL,
    created_at timestamp(6) without time zone,
    deleted_at timestamp(6) without time zone,
    harga_per_unit numeric(15,2) NOT NULL,
    max_qty integer NOT NULL,
    min_qty integer NOT NULL,
    updated_at timestamp(6) without time zone,
    id_produk uuid NOT NULL
);


ALTER TABLE public.tiering_prices OWNER TO admin_b2b;

--
-- Name: customers customers_pkey; Type: CONSTRAINT; Schema: public; Owner: admin_b2b
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_pkey PRIMARY KEY (id_user);


--
-- Name: deliveries deliveries_pkey; Type: CONSTRAINT; Schema: public; Owner: admin_b2b
--

ALTER TABLE ONLY public.deliveries
    ADD CONSTRAINT deliveries_pkey PRIMARY KEY (id_delivery);


--
-- Name: expiry_alerts expiry_alerts_pkey; Type: CONSTRAINT; Schema: public; Owner: admin_b2b
--

ALTER TABLE ONLY public.expiry_alerts
    ADD CONSTRAINT expiry_alerts_pkey PRIMARY KEY (id_alert);


--
-- Name: order_items order_items_pkey; Type: CONSTRAINT; Schema: public; Owner: admin_b2b
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT order_items_pkey PRIMARY KEY (id_order_item);


--
-- Name: orders orders_pkey; Type: CONSTRAINT; Schema: public; Owner: admin_b2b
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (id_order);


--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: admin_b2b
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id_payment);


--
-- Name: products products_pkey; Type: CONSTRAINT; Schema: public; Owner: admin_b2b
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_pkey PRIMARY KEY (id_produk);


--
-- Name: produk_batches produk_batches_pkey; Type: CONSTRAINT; Schema: public; Owner: admin_b2b
--

ALTER TABLE ONLY public.produk_batches
    ADD CONSTRAINT produk_batches_pkey PRIMARY KEY (id_prod_batch);


--
-- Name: tiering_prices tiering_prices_pkey; Type: CONSTRAINT; Schema: public; Owner: admin_b2b
--

ALTER TABLE ONLY public.tiering_prices
    ADD CONSTRAINT tiering_prices_pkey PRIMARY KEY (id_tiering);


--
-- Name: deliveries uke1wnounk8qmqwmoqvapvau14l; Type: CONSTRAINT; Schema: public; Owner: admin_b2b
--

ALTER TABLE ONLY public.deliveries
    ADD CONSTRAINT uke1wnounk8qmqwmoqvapvau14l UNIQUE (id_order);


--
-- Name: customers ukrfbvkrffamfql7cjmen8v976v; Type: CONSTRAINT; Schema: public; Owner: admin_b2b
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT ukrfbvkrffamfql7cjmen8v976v UNIQUE (email);


--
-- Name: order_items fk463h4eo5sqsq08idtys04dh2q; Type: FK CONSTRAINT; Schema: public; Owner: admin_b2b
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT fk463h4eo5sqsq08idtys04dh2q FOREIGN KEY (id_prod_batch) REFERENCES public.produk_batches(id_prod_batch);


--
-- Name: payments fkalvupkbf37ax1kxiayasm8k1x; Type: FK CONSTRAINT; Schema: public; Owner: admin_b2b
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fkalvupkbf37ax1kxiayasm8k1x FOREIGN KEY (id_order) REFERENCES public.orders(id_order);


--
-- Name: produk_batches fkc4hxwb92wdpexw41pac6igqq5; Type: FK CONSTRAINT; Schema: public; Owner: admin_b2b
--

ALTER TABLE ONLY public.produk_batches
    ADD CONSTRAINT fkc4hxwb92wdpexw41pac6igqq5 FOREIGN KEY (id_produk) REFERENCES public.products(id_produk);


--
-- Name: tiering_prices fkcayjxei9npv7b7k46kecjwf89; Type: FK CONSTRAINT; Schema: public; Owner: admin_b2b
--

ALTER TABLE ONLY public.tiering_prices
    ADD CONSTRAINT fkcayjxei9npv7b7k46kecjwf89 FOREIGN KEY (id_produk) REFERENCES public.products(id_produk);


--
-- Name: expiry_alerts fkh8wg7ktqptcenmsgj1lulr5r9; Type: FK CONSTRAINT; Schema: public; Owner: admin_b2b
--

ALTER TABLE ONLY public.expiry_alerts
    ADD CONSTRAINT fkh8wg7ktqptcenmsgj1lulr5r9 FOREIGN KEY (id_prod_batch) REFERENCES public.produk_batches(id_prod_batch);


--
-- Name: orders fkhvayjmb9hervngxqi60fsuv4s; Type: FK CONSTRAINT; Schema: public; Owner: admin_b2b
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT fkhvayjmb9hervngxqi60fsuv4s FOREIGN KEY (id_user) REFERENCES public.customers(id_user);


--
-- Name: deliveries fkicdv5oep7ix7gerw8kp3kkrg5; Type: FK CONSTRAINT; Schema: public; Owner: admin_b2b
--

ALTER TABLE ONLY public.deliveries
    ADD CONSTRAINT fkicdv5oep7ix7gerw8kp3kkrg5 FOREIGN KEY (id_order) REFERENCES public.orders(id_order);


--
-- Name: order_items fkj18ef1agdhkb3f8rmgrrgdbvu; Type: FK CONSTRAINT; Schema: public; Owner: admin_b2b
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT fkj18ef1agdhkb3f8rmgrrgdbvu FOREIGN KEY (id_order) REFERENCES public.orders(id_order);


--
-- PostgreSQL database dump complete
--

\unrestrict fMGEu86FbWTvZPf1lwFn02FZMuNSPPSJMaWd7lDfg0x3d4M7bcEd0whUYyKGuco

