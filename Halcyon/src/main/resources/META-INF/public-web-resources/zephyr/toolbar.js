import { enableDrawing } from './annotations/free-drawing.js';
import { rectangle } from './annotations/rectangle.js';
import { ellipse } from './annotations/ellipse.js';
import { polygon } from './annotations/polygon.js';
import { ruler } from './annotations/ruler.js';
import { grid } from './annotations/grid.js';
import { hollowBrush } from "./annotations/hollow-brush.js";
import { edit } from "./annotations/edit.js";
import { crosshairs } from "./helpers/crosshairs.js";
import { save } from "./annotations/save.js";
import { zoomControl, lockRotate } from "./helpers/zoomControl.js";
import { screenCapture } from "./helpers/elements.js";

export function toolbar(scene, camera, renderer, controls) {
  // Enable drawing on the scene
  enableDrawing(scene, camera, renderer, controls);
  rectangle(scene, camera, renderer, controls);
  ellipse(scene, camera, renderer, controls);
  polygon(scene, camera, renderer, controls);
  hollowBrush(scene, camera, renderer, controls);
  edit(scene, camera, renderer, controls);
  grid(scene, camera, renderer);
  ruler(scene, camera, renderer, controls);
  screenCapture(renderer);
  crosshairs(scene, camera);
  save(scene);
  lockRotate(controls);
  zoomControl(camera, controls);
}