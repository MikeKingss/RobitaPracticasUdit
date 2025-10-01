from flask import Flask, jsonify

app = Flask(__name__)

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
    print("Servidor Iniciando...")
    print("IP: 10.227.197.199:5000")
    app.run(host='0.0.0.0', port=5000, debug=True)