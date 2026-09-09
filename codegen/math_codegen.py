import re
import sympy as sp


n = int(input("n = "))


print(f"========== Matrix{n}f multiply(Matrix{n}f lhs, Matrix{n}f rhs) ==========")
for i in range(0, n):
    for j in range(0, n):
        components = []
        for k in range(0, n):
            components.append(f"lhs.m{i}{k} * rhs.m{k}{j}")
        value = f"{str.join(" + ", components)},"
        print(value)
    print()
print("================================\n\n")


print(f"========== Vector{n}f multiply(Matrix{n}f matrix{n}f, Vector{n}f vector{n}f) ==========")
for i in range(0, n):
    components = []
    for k in range(0, n):
        components.append(f"matrix{n}f.m{i}{k} * vector{n}f.v{k}()")
    value = f"{str.join(" + ", components)},"
    print(value)
print("================================\n\n")


print(f"========== Matrix{n}f transpose() ==========")
for i in range(0, n):
    components = []
    for j in range(0, n):
        components.append(f"m{j}{i}")
    value = f"{str.join(", ", components)},"
    print(value)
print("================================\n\n")


print(f"========== Matrix{n}f det() ==========")
M = sp.Matrix(n, n, lambda i, j: sp.Symbol(f"m{i}{j}"))
det_expr = sp.simplify(M.det())
result = str(det_expr)
result = re.sub(r"([+-])", r"\1\n", result)
result = re.sub(r"\*", r" * ", result)
print(result)
print("================================\n\n")


print(f"========== Matrix{n}f invert() ==========")
M = sp.Matrix(n, n, lambda i, j: sp.Symbol(f"m{i}{j}"))
adj_M = sp.simplify(M.adjugate())

for i in range(adj_M.rows):
    for j in range(adj_M.cols):
        cell_expr = adj_M[i, j]
        result = str(cell_expr)
        result = re.sub(r"([+-]?)([^+-]+)\s?", r"                \1\2\n", result)
        result = re.sub(r"\*", r" * ", result)
        print(f"                // m{i}{j}")
        print(f"                ({result.rstrip("\n").lstrip()}) / det,\n")
print("================================\n\n")


