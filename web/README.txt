HOW TO ADD NEW CARS
===================

1. Open the 'cars.json' file in a text editor.

2. Add a new entry to the list following this format:

   {
       "id": 5,
       "name": "New Car Name",
       "price": 100,
       "image": "images/your-car-photo.jpg",
       "features": ["Feature 1", "Feature 2"]
   }

3. IMPORTANT: IMAGES MUST BE JPEG FORMAT
   - The system only supports .jpg or .jpeg files.
   - Place your image files in the 'images' folder.
   - Make sure the "image" path in cars.json points to "images/filename.jpg".

4. Save 'cars.json' and refresh the web page to see your new car.
