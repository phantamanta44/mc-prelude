#!/usr/bin/env python3

from math import pi, sin, cos, tan

def roundish(x: float) -> str:
  xr = round(x)
  return xr if abs(xr - x) <= 0.0001 else x

class Turtle:
  def __init__(self):
    self.x = 0.0
    self.y = 0.0
    self.heading = 0.0
    self.log: list[str] = []

  def set_pos(self, x: float, y: float):
    self.x = x
    self.y = y
    self.log.append(f"M {roundish(x)} {roundish(y)}")

  def get_pos(self) -> tuple[float, float]:
    return self.x, self.y

  def set_heading(self, heading: float):
    self.heading = heading

  def teleport(self, x: float, y: float, heading: float):
    self.set_pos(x, y)
    self.set_heading(heading)

  def turn(self, offset: float):
    self.heading += offset

  def move(self, dist: float):
    x_off = dist * cos(self.heading)
    y_off = dist * sin(self.heading)
    self.x += x_off
    self.y += y_off
    self.log.append(f"l {roundish(x_off)} {roundish(y_off)}")

  def move_to(self, x: float, y: float):
    self.log.append(f"l {roundish(x - self.x)} {roundish(y - self.y)}")
    self.x = x
    self.y = y

  def close(self):
    self.log.append("Z")

  def dump(self):
    path = " ".join(self.log)
    print(f"<path d=\"{path}\"/>")
    self.log = []

def main():
  sq_edge_1 = 128 # length of horizontal edges on front/back faces
  sq_edge_2 = 136 # length of vertical edges on front/back faces
  conn_edge = 116 # length of connecting edges between front/back faces
  inner_ang = 64 * pi / 180 # angle between horizontal and vertical edges on front/back faces
  outer_ang = pi / 3 # angle between horizontal edges on front/back faces and connecting edges
  lambda_gap = 24 # gap between cube and lambda
  lambda_thickness = 40 # thickness of the lambda's fill

  turtle = Turtle()

  # draw outer hexagon
  turtle.teleport(conn_edge * cos(outer_ang), 0, 0)
  ux1, uy1 = turtle.get_pos() # top-left of back face
  turtle.move(sq_edge_1)
  ux2, uy2 = turtle.get_pos() # top-right of back face
  turtle.turn(inner_ang)
  turtle.move(sq_edge_2)
  ux3, uy3 = turtle.get_pos() # bottom-right of back face
  turtle.turn(pi - inner_ang - outer_ang)
  turtle.move(conn_edge)
  lx3, ly3 = turtle.get_pos() # bottom-right of front face
  turtle.turn(outer_ang)
  turtle.move(sq_edge_1)
  lx4, ly4 = turtle.get_pos() # bottom-left of front face
  turtle.turn(inner_ang)
  turtle.move(sq_edge_2)
  lx1, ly1 = turtle.get_pos() # top-left of front face
  turtle.close()
  turtle.dump()

  # draw inner edges
  turtle.teleport(lx4, ly4, -outer_ang)
  turtle.move(conn_edge)
  bx, by = turtle.get_pos() # back edge meet point
  turtle.set_heading(0)
  turtle.move(sq_edge_1)
  turtle.teleport(bx, by, pi + inner_ang)
  turtle.move(sq_edge_2)

  turtle.teleport(ux2, uy2, pi - outer_ang)
  turtle.move(conn_edge)
  fx, fy = turtle.get_pos() # front edge meet point
  turtle.set_heading(pi)
  turtle.move(sq_edge_1)
  turtle.teleport(fx, fy, inner_ang)
  turtle.move(sq_edge_2)
  turtle.dump()

  # compute vertex positions for lambda
  ih = ly3 # image height (not accounting for stroke width)
  lux = ux2 + lambda_gap # top-left vertex of lambda
  luy = uy2
  llx = lx3 + lambda_gap # bottom-left vertex of lambda
  lly = ly3
  ix1 = ux3 + lambda_gap # middle-left vertex of lambda
  iy1 = uy3

  tani = tan(inner_ang)
  tano = tan(outer_ang)
  ix2 = (tani*lux + tano*(llx+lambda_thickness) + lly - luy) / (tani + tano) # middle-lower vertex of lambda
  iy2 = tani * (ix2 - lux) + luy

  # draw lambda
  turtle.teleport(lux, luy, 0)
  turtle.move(lambda_thickness)
  turtle.turn(inner_ang)
  turtle.move(ih / sin(inner_ang))
  iw = turtle.get_pos()[0] # image width (not accounting for stroke width)
  turtle.turn(pi - inner_ang)
  turtle.move(lambda_thickness)
  turtle.turn(inner_ang)
  turtle.move_to(ix2, iy2)
  turtle.turn(-inner_ang - outer_ang)
  turtle.move_to(llx + lambda_thickness, lly)
  turtle.move_to(llx, lly)
  turtle.set_heading(-outer_ang)
  turtle.move_to(ix1, iy1)
  turtle.close()
  turtle.dump()

  # print viewport offset required to center image in a 400x400 square
  print(f"{(400 - iw) / 2} {(400 - ih) / 2}")

if __name__ == "__main__":
  main()
