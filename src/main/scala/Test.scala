import jp.kobe_u.scarab._

object Test {
  def main(args: Array[String]) {
    test1  
//    println("============")
//    test2

//    test3
  }

  def test1 {
    import jp.kobe_u.scarab._, dsl._

    use(new Sat4j("xplain"))

    def define(sum: Int) {
      reset
      boolInt('x(2)); boolInt('x(3)); boolInt('x(5)); boolInt('x(8)); boolInt('x(13)); boolInt('x(21)); boolInt('x(34))
      add('x(2) * 2 + 'x(3) * 3 + 'x(5) * 5 + 'x(8) * 8 + 'x(13) * 13 + 'x(21) * 21 + 'x(34) * 34 === sum)
    }  

    define(50)

    add('x(2) === 1)
    add('x(21) === 1)
    add('x(34) === 1)

    find

    for (i <- minExplain)
      println(i)

  }
  
  def test2 {
    import jp.kobe_u.scarab._, dsl._

    use(new Sat4j("xplain"))

    def define(sum: Int) {
      reset
      boolInt('x(2)); boolInt('x(3)); boolInt('x(5)); boolInt('x(8)); boolInt('x(13)); boolInt('x(21)); boolInt('x(34))
      add('x(2) * 2 + 'x(3) * 3 + 'x(5) * 5 + 'x(8) * 8 + 'x(13) * 13 + 'x(21) * 21 + 'x(34) * 34 === sum)
    }

    define(50)
    
    find    

    add('x(2) === 1)
    add('x(21) === 1)
    add('x(34) === 1)

    find

    for (i <- minExplain)
      println(i)

  }
  
  def test3 {
    import jp.kobe_u.scarab._, dsl._    

    def define(sum: Int) {
      reset
      boolInt('x(2)); boolInt('x(3)); boolInt('x(5)); boolInt('x(8)); boolInt('x(13)); boolInt('x(21)); boolInt('x(34))
      add('x(2) * 2 + 'x(3) * 3 + 'x(5) * 5 + 'x(8) * 8 + 'x(13) * 13 + 'x(21) * 21 + 'x(34) * 34 === sum)
    }

    define(50)

    while(findNext)
      println(solution.intMap)
    
  }
  
  

}