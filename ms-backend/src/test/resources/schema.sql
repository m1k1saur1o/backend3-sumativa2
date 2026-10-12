CREATE TABLE cuentas_anuales (id BIGINT AUTO_INCREMENT PRIMARY KEY, cuenta_id VARCHAR(50) NOT NULL,
  fecha DATE NOT NULL, transaccion VARCHAR(50) NOT NULL, monto DECIMAL(15,2) NOT NULL, descripcion VARCHAR(255));
CREATE TABLE intereses (id BIGINT AUTO_INCREMENT PRIMARY KEY, cuenta_id VARCHAR(50) NOT NULL,
  nombre VARCHAR(150) NOT NULL, saldo DECIMAL(15,2) NOT NULL, edad INT NOT NULL, tipo VARCHAR(50) NOT NULL);
CREATE TABLE transacciones (registro_id BIGINT AUTO_INCREMENT PRIMARY KEY, id VARCHAR(50) NOT NULL,
  fecha DATE NOT NULL, monto DECIMAL(15,2) NOT NULL, tipo VARCHAR(50) NOT NULL);
INSERT INTO cuentas_anuales (cuenta_id, fecha, transaccion, monto, descripcion) VALUES
  ('110', '2024-11-23', 'compra', 1500.00, 'Ingreso mensual'),
  ('110', '2024-12-22', 'deposito', 2500.00, 'Deposito');
