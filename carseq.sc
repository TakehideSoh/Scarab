import jp.kobe_u.scarab._ ; import dsl._ 

val src = scala.io.Source.fromFile(args(0))
val lines = src.getLines.toSeq

/*
 * ==============================
 * (1) Parsing Input File
 * ==============================
 * 
 * explanation from: http://www.csplib.org/Problems/prob001/
 *
 * 1st line: number of cars; number of options; number of classes.
 * 2nd line: for each option, the maximum number of cars with that option in a block.
 * 3rd line: for each option, the block size to which the maximum number refers.
 * Then for each class: index no.; no. of cars in this class; for each option, whether or not this class requires it (1 or 0).
 * */

// 1st line
val first_line = lines(0).split(' ')
val number_of_cars = first_line(0).toInt
val number_of_options = first_line(1).toInt
val number_of_classes = first_line(2).toInt

// 2nd line
val second_line = lines(1).split(' ')
var max_number_of_cars_for_options = for (i <- second_line) yield i.toInt

// 3rd line
val third_line = lines(2).split(' ')
val blocks_size_for_options = for (i <- third_line) yield i.toInt

// 4th line and after
val class_spec = 
  for {
    i <- 3 until 3+number_of_classes
    is = lines(i).split(' ')
    number_of_cars_in_this_class = is(1).toInt
    options_required_or_not = for (j <- 2 until 2 + number_of_options) yield is(j).toInt
  } yield (number_of_cars_in_this_class, options_required_or_not)

/*
 * ==============================
 * (2) Defining Integer Variables
 * ==============================
 * */

/*
 * (2-1) 0-1 variable c_i_j means "car of class k" is located to "i-th position" */
val c = int('c,0,1)
for (k <- 0 until number_of_classes; i <- 0 until number_of_cars)
  int(c(k,i),0,1)

/*
 * (2-*) TODO defining remaining variables
 * */

/*
 * ==============================
 * (3) Defining Constraitns
 * ==============================
 * */
/*
 * (3-1) for each class, 
 * */
for (class_id <- 0 until class_spec.size) {
  val xs = for (i <- 0 until number_of_cars) yield c(class_id,i)
  add(Sum(xs) === class_spec(class_id)._1)
}

/*
 * (3-*) TODO defining remaining constraints
 * */

// "show" method print all variables and constrains added so far
show

/*
 * ==============================
 * (4) Solve CSP and print solution if any
 * ==============================
 * */
if (find) {
  println(solution)
}
