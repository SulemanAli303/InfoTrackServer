import serial
import time

# Configure the serial connection
def send_sms(port, phone_number, message):
    try:
        # Open the serial port
        ser = serial.Serial(port, baudrate=9600, timeout=5)
        time.sleep(2)  # Wait for the modem to initialize

        # Check if the modem is ready
        ser.write(b'AT\r')
        response = ser.read(100)
        if b'OK' not in response:
            print("Modem not responding.")
            return

        # Set the modem to text mode
        ser.write(b'AT+CMGF=1\r')
        time.sleep(1)

        # Send the SMS
        ser.write(f'AT+CMGS="{phone_number}"\r'.encode())
        time.sleep(1)
        ser.write(f'{message}\r'.encode())
        time.sleep(1)
        ser.write(bytes([26]))  # Send Ctrl+Z to indicate the end of the message
        time.sleep(2)

        # Read the response
        response = ser.read(100)
        if b'OK' in response:
            print("SMS sent successfully!")
        else:
            print("Failed to send SMS.")

        # Close the serial port
        ser.close()

    except Exception as e:
        print(f"Error: {e}")


# Example usage
if __name__ == "__main__":
    port = "/dev/ttyu0"  # Replace with your modem's port (e.g., COM3 on Windows)
    phone_number = "+923027655876"  # Replace with the recipient's phone number
    message = "Hello from Python and GSM modem!"

    send_sms(port, phone_number, message)