package com.microservice.document_processing_service.service.ai;

import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    public String buildClassificationPrompt(String ocrText) {
        return """
            You are an expert in receipt analysis. Given the following text extracted from a receipt, classify each item into a category (choose from the provided list) and extract the type, price, date, description, payment method. If any field is missing, estimate it reasonably or set it to null. Return the result as a JSON array of objects with fields: "name", "category", "type", "price", "date", "description", "paymentMethod". Ensure the output is valid JSON.
            
            Categories:
            - Groceries
            - Restaurants
            - Transport
            - Entertainment
            - Utilities
            - Shopping
            - Healthcare
            - Education
            - Travel
            - Subscriptions
            - Rent
            - Taxes
            - Gifts Given
            - Pets
            - Hobbies
            - Insurance
            - Repairs
            - Business Expenses
            - Childcare
            - Debt Repayment
            - Salary
            - Freelance
            - Investments
            - Bonuses
            - Gifts Received
            - Refunds
            - Passive Income
            - Sales
            - Grants
            - Cashback
            
            Type of it:
            - EXPENSE
            - INCOME
            
            Payment type:
            - Credit Card
            - Cash

            Receipt text:
            %s

            ### **Important Rules:**
            1. If the receipt contains an **itemized list**, only return the individual items and **exclude the total amount**
            2. If there are **no items**, return only the **total amount** as a single transaction.
            3. If name of item is seems to be written with mistake, just solve it and provide approximately correct name in correct language
            
            Example output:
            [
                {"name": "Milk", "type": "EXPENSE", "category": "Groceries", "price": 2.50, "date": "2024-03-20T12:00:00Z", "description": "Organic milk", "paymentMethod": "Credit Card" },
                {"name": "Bus Ticket", "type": "EXPENSE", "category": "Transport", "price": 1.20, "date": "2024-03-20T12:30:00Z", "description": "City bus", "paymentMethod": "Cash"}
            ]
            
            If it not type of receipt in text, or it seems to be nonsense text give output
            [
                {"name": "NONSENSE"}
            ]
            """.formatted(ocrText);
    }
}