import { useState } from "react";

const images = [
  "https://picsum.photos/800/800",
  "https://picsum.photos/801/800",
  "https://picsum.photos/802/800",
  "https://picsum.photos/803/800",
];

export default function ProductGallery() {
  const [mainImage, setMainImage] = useState(images[0]);

  return (
    <div>
      <div className="bg-gray-900 rounded-3xl overflow-hidden">
        <img
          src={mainImage}
          alt="Product"
          className="w-full aspect-square object-cover"
        />
      </div>

      <div className="flex gap-3 mt-6 overflow-x-auto pb-2">
        {images.map((img) => (
          <img
            key={img}
            src={img}
            onClick={() => setMainImage(img)}
            className={`w-20 h-20 md:w-24 md:h-24 rounded-2xl object-cover cursor-pointer border-2 flex-shrink-0
              ${
                mainImage === img
                  ? "border-[#FFCC00]"
                  : "border-gray-700 hover:border-[#FFCC00]"
              }`}
          />
        ))}
      </div>
    </div>
  );
}