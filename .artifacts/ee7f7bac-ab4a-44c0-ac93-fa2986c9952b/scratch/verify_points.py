def calculate_repio_points(exercise_name, weight_kg, reps, is_bw):
    if "plank" in exercise_name.lower() or "hold" in exercise_name.lower():
        return 0.0

    if is_bw and weight_kg == 0.0:
        return 1.0 * reps
    else:
        return weight_kg * reps

test_cases = [
    ("Bench Press", 100.0, 10, False, 1000.0),
    ("Push-ups", 0.0, 20, True, 20.0),
    ("Weighted Push-ups", 10.0, 15, True, 150.0),
    ("Plank", 0.0, 60, True, 0.0),
]

for name, weight, reps, is_bw, expected in test_cases:
    actual = calculate_repio_points(name, weight, reps, is_bw)
    print(f"{name}: Weight={weight}, Reps={reps}, BW={is_bw} | Expected={expected}, Actual={actual} | {'PASS' if actual == expected else 'FAIL'}")
