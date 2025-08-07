import random
import requests
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import List, Dict, Tuple

# Configuration
BASE_URL = "https://search-system-jced.onrender.com/api/v1/dishes"  # Update this if needed
NUM_DISHES = 200  # Total number of dishes to add
MAX_WORKERS = 10  # Concurrency

# Sample data
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
    cuisine = random.choice(CUISINES)
    dish_type = random.choice(DISH_TYPES[cuisine])

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
    try:
        response = requests.post(BASE_URL, json=dish)
        if response.status_code == 200:
            return True, f"✅ Added: {dish['name']} ({dish['category']})"
        else:
            return False, f"❌ Failed: {dish['name']} - {response.status_code}"
    except Exception as e:
        return False, f"⚠️ Error: {dish['name']} - {str(e)}"


def add_dishes_batch(dishes: List[Dict[str, str]]) -> Tuple[int, int]:
    success = 0
    failures = 0

    with ThreadPoolExecutor(max_workers=min(MAX_WORKERS, len(dishes))) as executor:
        futures = {executor.submit(add_dish, dish): dish for dish in dishes}
        for future in as_completed(futures):
            is_success, msg = future.result()
            print(msg)
            if is_success:
                success += 1
            else:
                failures += 1

    return success, failures


def main():
    print(f"🚀 Starting to add {NUM_DISHES} dishes to the search service...\n")

    total_success = 0
    total_failures = 0
    processed = 0
    recent_dishes = []
    start_time = time.time()

    while processed < NUM_DISHES:
        batch_size = random.randint(10, 75)
        batch_size = min(batch_size, NUM_DISHES - processed)

        print(f"\n📦 Processing batch of {batch_size} dishes ({(processed / NUM_DISHES * 100):.1f}% complete)...")

        dishes = []
        for _ in range(batch_size):
            if recent_dishes and random.random() < 0.02:
                # 2% chance to duplicate from recent
                dishes.append(random.choice(recent_dishes))
            else:
                dish = generate_dish()
                dishes.append(dish)
                recent_dishes.append(dish)

        success, failures = add_dishes_batch(dishes)
        total_success += success
        total_failures += failures
        processed += batch_size

        time.sleep(random.uniform(0.1, 0.5))  # Random sleep between batches

    elapsed = time.time() - start_time
    print("\n" + "=" * 60)
    print(f"✅ Completed in {elapsed:.2f} seconds")
    print(f"🟢 Total dishes added: {total_success}")
    print(f"🔴 Total failures: {total_failures}")
    print(f"📊 Success rate: {(total_success / (total_success + total_failures) * 100):.1f}%")
    print("=" * 60)


if __name__ == "__main__":
    main()
