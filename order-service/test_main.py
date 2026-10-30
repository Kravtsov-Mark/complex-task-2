import requests
from fastapi.testclient import TestClient
import main

client = TestClient(main.app)

# Сценарій 1: Успішне створення замовлення
def test_create_order_success(requests_mock):
    requests_mock.get("http://localhost:8080/products/1", json={"id": 1, "name": "Laptop", "price": 1200.50})
    requests_mock.get("http://localhost:8080/products/2", json={"id": 2, "name": "Smartphone", "price": 800.00})

    order_payload = [
        {"product_id": 1, "quantity": 1},
        {"product_id": 2, "quantity": 2}
    ]

    response = client.post("/orders", json=order_payload)
    assert response.status_code == 201
    response_data = response.json()
    assert response_data["items"][0]["product_id"] == 1
    assert response_data["items"][1]["product_id"] == 2
    assert response_data["total_amount"] == 2800.50

# Сценарій 2: Спроба створити замовлення з неіснуючим продуктом
def test_create_order_product_not_found(requests_mock):
    requests_mock.get("http://localhost:8080/products/99", status_code=404)
    order_payload = [{"product_id": 99, "quantity": 1}]

    response = client.post("/orders", json=order_payload)
    assert response.status_code == 400
    assert "not found" in response.json()["detail"].lower()

# Сценарій 3: Каталог продуктів недоступний
def test_create_order_product_service_unavailable(requests_mock):
    requests_mock.get("http://localhost:8080/products/1", exc=requests.exceptions.ConnectionError)
    order_payload = [{"product_id": 1, "quantity": 1}]

    response = client.post("/orders", json=order_payload)
    assert response.status_code == 503
    assert "Cannot connect" in response.json()["detail"]
