import os
from datetime import date, timedelta
from typing import List

import requests
from fastapi import Depends, FastAPI, HTTPException, status
from pydantic import BaseModel
from sqlalchemy import Column, Float, ForeignKey, Integer, String, create_engine
from sqlalchemy.orm import Session, declarative_base, relationship, sessionmaker

PRODUCT_SERVICE_URL = "http://localhost:8080/products"

# Для тестов берется SQLite из переменной окружения, для обычной работы — твоя MySQL
DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "mysql+pymysql://root:root@localhost:3306/microservices_db"
)

connect_args = {"check_same_thread": False} if DATABASE_URL.startswith("sqlite") else {}
engine = create_engine(DATABASE_URL, connect_args=connect_args)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


# Таблицы в базе данных MySQL
class OrderDB(Base):
    __tablename__ = "orders"

    id = Column(Integer, primary_key=True, index=True)
    total_amount = Column(Float, nullable=False, default=0.0)
    deliveryDate = Column(String(50), nullable=False)
    items = relationship("OrderItemDB", back_populates="order", cascade="all, delete-orphan")


class OrderItemDB(Base):
    __tablename__ = "order_items"

    id = Column(Integer, primary_key=True, index=True)
    order_id = Column(Integer, ForeignKey("orders.id"), nullable=False)
    product_id = Column(Integer, nullable=False)
    quantity = Column(Integer, nullable=False)
    order = relationship("OrderDB", back_populates="items")


# Автоматически создает таблицы orders и order_items в microservices_db
Base.metadata.create_all(bind=engine)

app = FastAPI()


# Pydantic-схемы (оставлены без изменений)
class OrderItem(BaseModel):
    product_id: int
    quantity: int


class Order(BaseModel):
    id: int
    items: List[OrderItem]
    total_amount: float = 0.0
    deliveryDate: str


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def to_order_schema(db_order: OrderDB) -> Order:
    return Order(
        id=db_order.id,
        items=[
            OrderItem(product_id=item.product_id, quantity=item.quantity)
            for item in db_order.items
        ],
        total_amount=db_order.total_amount,
        deliveryDate=db_order.deliveryDate,
    )


@app.get("/orders", response_model=List[Order])
def get_all_orders(db: Session = Depends(get_db)):
    db_orders = db.query(OrderDB).all()
    return [to_order_schema(order) for order in db_orders]


@app.get("/orders/{order_id}", response_model=Order)
def get_order_by_id(order_id: int, db: Session = Depends(get_db)):
    db_order = db.query(OrderDB).filter(OrderDB.id == order_id).first()
    if not db_order:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Order not found"
        )
    return to_order_schema(db_order)


@app.post("/orders", response_model=Order, status_code=status.HTTP_201_CREATED)
def create_order(order_items: List[OrderItem], db: Session = Depends(get_db)):
    total_amount = 0.0
    validated_items = []

    for item in order_items:
        try:
            response = requests.get(f"{PRODUCT_SERVICE_URL}/{item.product_id}")

            if response.status_code == 200:
                product_data = response.json()
                total_amount += product_data["price"] * item.quantity
                validated_items.append(item)
            elif response.status_code == 404:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail=f"Product with id {item.product_id} not found."
                )
            else:
                raise HTTPException(
                    status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                    detail="Product service is unavailable."
                )

        except requests.exceptions.RequestException:
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="Cannot connect to Product service."
            )

    delivery_date = (date.today() + timedelta(days=5)).isoformat()

    db_order = OrderDB(
        total_amount=total_amount,
        deliveryDate=delivery_date,
        items=[
            OrderItemDB(product_id=item.product_id, quantity=item.quantity)
            for item in validated_items
        ]
    )
    db.add(db_order)
    db.commit()
    db.refresh(db_order)

    return to_order_schema(db_order)