import bpy, bmesh, collections, json
from mathutils import Vector
o = bpy.data.objects['GEO-body_male_realistic']; m = o.data
fs = [v.value for v in m.attributes['.sculpt_face_set'].data]
cent = collections.defaultdict(lambda: [Vector(), 0])
for p in m.polygons:
    c = cent[fs[p.index]]; c[0] += p.center; c[1] += 1
vert_sets = collections.defaultdict(set)
for p in m.polygons:
    for vi in p.vertices: vert_sets[vi].add(fs[p.index])
bound = collections.defaultdict(list)
for vi, s in vert_sets.items():
    if len(s) == 2:
        bound[tuple(sorted(s))].append(m.vertices[vi].co)
out = {"sets": {k: [list(map(lambda x: round(x, 4), v[0] / v[1])), v[1]] for k, v in cent.items()},
       "joints": {f"{a}-{b}": [list(map(lambda x: round(x, 4), sum(vs, Vector()) / len(vs))), len(vs)] for (a, b), vs in bound.items()}}
json.dump(out, open("/home/matrix/StudioProjects/RepIo/animation/output/facesets.json", "w"), indent=0)
print("OK", len(out["sets"]), len(out["joints"]))
