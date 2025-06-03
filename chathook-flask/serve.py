from waitress import serve
import app

if __name__ == "__main__":
    # listens on 0.0.0.0:5000 so port-forwarded traffic can reach it
    serve(app.app, host="0.0.0.0", port=5000)
