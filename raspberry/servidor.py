from flask import Flask, jsonify
import socket

app = Flask(__name__)

def obtener_ip():
    """Detecta automáticamente la IP actual de la Raspberry Pi"""
    try:
        # Crea un socket temporal para detectar qué IP local se usa
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))  # No envía datos, solo detecta la interfaz
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"  # Fallback si no puede detectar

@app.route('/')
def inicio():
    return "Servidor Funcionando!"

@app.route('/hola')
def hola():
    return jsonify({
        'mensaje': 'Hola desde la Raspberry Pi!',
        'estado': 'conectado'
    })

@app.route('/led/on')
def led_on():
    print("LED encendido")
    return jsonify({
        'led': 'encendido',
        'estado': 'ok'
    })

@app.route('/led/off')
def led_off():
    print("LED apagado")
    return jsonify({
        'led': 'apagado',
        'estado': 'ok'
    })

if __name__ == '__main__':
    ip_actual = obtener_ip()
    print("Servidor Iniciando...")
    print(f"IP: {ip_actual}:5000")
    app.run(host='0.0.0.0', port=5000, debug=True)