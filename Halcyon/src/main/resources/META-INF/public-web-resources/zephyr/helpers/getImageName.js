import { getUrl } from "./conversions.js";

export function getImageName(scene) {
  addEventListener("DOMContentLoaded", (event) => {
    const checkInterval = 1000; // 1 second
    const maxAttempts = 10; // Stop after 10 attempts (10 seconds)

    let attempt = 0;

    const checkUrl = () => {
      const url = getUrl(scene);
      if (url) {
        const imageName = url.split("/").pop();
        // const parts = imageName.split(".");
        // const firstPart = parts[0];
        const textNode = document.createTextNode(`Image: ${imageName}`);
        const divElement = document.createElement("div");
        divElement.style.display = "inline-block";
        divElement.style.paddingLeft = "10px";
        divElement.appendChild(textNode);
        let canvas = document.querySelector('canvas');
        document.body.insertBefore(divElement, canvas);
      } else if (attempt < maxAttempts) {
        attempt++;
        setTimeout(checkUrl, checkInterval);
      } else {
        console.error("Max attempts reached. Unable to get image URL.");
      }
    };

    checkUrl();
  });
}
