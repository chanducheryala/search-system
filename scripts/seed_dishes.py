import random
import requests
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import List, Dict, Tuple

# Configuration
BASE_URL = "http://localhost:8080/api/v1/dishes"  # Update if your service runs on a different port
BATCH_SIZE = 50  # Number of dishes to add in each batch
NUM_DISHES = 30000  # Total number of dishes to add
MAX_WORKERS = 10  # Number of concurrent workers for parallel processing

# Sample data for generating random dishes
CUISINES = ["Italian", "Indian", "Chinese", "Mexican", "Japanese", "Thai", "French", "Mediterranean", "American", "Middle Eastern"]

DISH_TYPES = {
    "Italian": ["Pasta", "Pizza", "Risotto", "Lasagna", "Ravioli", "Gnocchi", "Osso Buco", "Tiramisu", "Gelato", "Bruschetta"],
    "Indian": ["Butter Chicken", "Biryani", "Tikka Masala", "Samosas", "Dosa", "Vindaloo", "Korma", "Naan", "Tandoori", "Kheer"],
    "Chinese": ["Dumplings", "Kung Pao Chicken", "Fried Rice", "Hot Pot", "Peking Duck", "Mapo Tofu", "Spring Rolls", "Wonton Soup", "Char Siu", "Dan Dan Noodles"],
    "Mexican": ["Tacos", "Burritos", "Quesadillas", "Enchiladas", "Tamales", "Chiles Rellenos", "Guacamole", "Pozole", "Tostadas", "Churros"],
    "Japanese": ["Sushi", "Ramen", "Tempura", "Udon", "Sashimi", "Tonkatsu", "Okonomiyaki", "Yakitori", "Miso Soup", "Matcha Ice Cream"],
    "Thai": ["Pad Thai", "Green Curry", "Tom Yum", "Massaman Curry", "Som Tum", "Pad See Ew", "Tom Kha Gai", "Mango Sticky Rice", "Satay", "Khao Soi"],
    "French": ["Coq au Vin", "Bouillabaisse", "Ratatouille", "Quiche Lorraine", "Croissant", "Boeuf Bourguignon", "Crème Brûlée", "Soufflé", "Cassoulet", "Tarte Tatin"],
    "Mediterranean": ["Hummus", "Falafel", "Shawarma", "Moussaka", "Tabbouleh", "Baklava", "Dolma", "Gyro", "Spanakopita", "Baba Ganoush"],
    "American": ["Hamburger", "Hot Dog", "BBQ Ribs", "Mac & Cheese", "Fried Chicken", "Apple Pie", "Clam Chowder", "Buffalo Wings", "Pancakes", "Cornbread"],
    "Middle Eastern": ["Kebab", "Shish Tawook", "Mansaf", "Fattoush", "Knafeh", "Mujadara", "Falafel", "Shakshuka", "Ful Medames", "Baklava"]
}

ADJECTIVES = ["Spicy", "Creamy", "Crispy", "Grilled", "Roasted", "Steamed", "Fried", "Baked", "Smoked", "Marinated"]
INGREDIENTS = ["Chicken", "Beef", "Vegetable", "Seafood", "Tofu", "Paneer", "Mushroom", "Lamb", "Pork", "Fish"]


def generate_dish() -> Dict[str, str]:
    """Generate a random dish with name and category."""
    cuisine = random.choice(CUISINES)
    dish_type = random.choice(DISH_TYPES[cuisine])
    
    # Sometimes add an adjective or ingredient for variety
    if random.random() > 0.5:
        prefix = random.choice(ADJECTIVES)
        dish_name = f"{prefix} {dish_type}"
    elif random.random() > 0.7:
        ingredient = random.choice(INGREDIENTS)
        dish_name = f"{ingredient} {dish_type}"
    else:
        dish_name = dish_type
    
    return {"name": dish_name, "category": cuisine}


def add_dish(dish: Dict[str, str]) -> Tuple[bool, str]:
    """Add a single dish to the search service."""
    try:
        response = requests.post(BASE_URL, json=dish)
        if response.status_code == 200:
            return True, f"Added: {dish['name']} ({dish['category']})"
        else:
            return False, f"Failed to add {dish['name']}: {response.status_code} - {response.text}"
    except Exception as e:
        return False, f"Error adding {dish['name']}: {str(e)}"


def add_dishes_batch(dishes: List[Dict[str, str]]) -> Tuple[int, int]:
    """Add a batch of dishes and return success/failure counts."""
    success = 0
    failures = 0
    
    with ThreadPoolExecutor(max_workers=min(MAX_WORKERS, len(dishes))) as executor:
        future_to_dish = {executor.submit(add_dish, dish): dish for dish in dishes}
        for future in as_completed(future_to_dish):
            is_success, message = future.result()
            if is_success:
                success += 1
            else:
                failures += 1
            print(message)
    
    return success, failures


def main():
    print(f"Starting to add {NUM_DISHES} dishes to the search service...")
    
    total_success = 0
    total_failures = 0
    start_time = time.time()
    
    # Process in batches to manage memory and show progress
    for i in range(0, NUM_DISHES, BATCH_SIZE):
        batch_size = min(BATCH_SIZE, NUM_DISHES - i)
        print(f"\nProcessing batch of {batch_size} dishes ({(i / NUM_DISHES * 100):.1f}% complete)...")
        
        # Generate a batch of dishes
        dishes = [generate_dish() for _ in range(batch_size)]
        
        # Add the batch and track results
        success, failures = add_dishes_batch(dishes)
        total_success += success
        total_failures += failures
        
        # Add a small delay between batches to avoid overwhelming the server
        time.sleep(0.5)
    
    # Print summary
    elapsed_time = time.time() - start_time
    print("\n" + "=" * 50)
    print(f"Completed in {elapsed_time:.2f} seconds")
    print(f"Total dishes added: {total_success}")
    print(f"Total failures: {total_failures}")
    print(f"Success rate: {(total_success / (total_success + total_failures) * 100):.1f}%")
    print("=" * 50)


if __name__ == "__main__":
    main()
