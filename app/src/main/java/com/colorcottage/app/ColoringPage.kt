package com.colorcottage.app

enum class ColoringPage(
    val title: String,
    val imageRes: Int,
    val vectorRasterWidth: Int = 0,
    val vectorRasterHeight: Int = 0
) {
    PUPPY("Puppy", R.drawable.animal_puppy_vector, 1198, 1313),
    KITTEN("Kitten", R.drawable.animal_kitten_vector, 1206, 1304),
    BUNNY("Bunny", R.drawable.animal_bunny_vector, 1198, 1313),
    DUCKLING("Duckling", R.drawable.animal_duckling_vector, 1199, 1312),
    PONY("Pony", R.drawable.animal_pony_vector, 1, 1),
    DINOSAUR("Baby Dinosaur", R.drawable.animal_dinosaur_vector, 1, 1),

    LAMB("Lamb", R.drawable.animal_lamb_vector, 1, 1),
    PIGLET("Piglet", R.drawable.animal_piglet_vector, 1, 1),
    CHICK("Chick", R.drawable.animal_chick_vector, 1, 1),
    CALF("Calf", R.drawable.animal_calf_vector, 1, 1),

    KOALA("Koala Joey", R.drawable.animal_koala_vector, 1, 1),
    OTTER("Otter Pup", R.drawable.animal_otter_vector, 1, 1),
    HEDGEHOG("Hedgehog", R.drawable.animal_hedgehog_vector, 1, 1),
    RACCOON("Raccoon Kit", R.drawable.animal_raccoon_vector, 1, 1),
    FOX("Fox Cub", R.drawable.animal_fox_vector, 1, 1),
    BEAR("Bear Cub", R.drawable.animal_bear_vector, 1, 1),
    TURTLE("Turtle", R.drawable.animal_turtle_vector, 1, 1),
    PANDA("Panda", R.drawable.animal_panda_vector, 1, 1)
}
