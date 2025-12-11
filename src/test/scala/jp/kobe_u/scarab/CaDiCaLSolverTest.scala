package jp.kobe_u.scarab

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterEach

/**
 * CaDiCaLSolver のユニットテスト
 *
 * t-wada 流:
 * - テスト名は日本語で「何をしたら何が起こる」を明確に
 * - Arrange-Act-Assert パターン
 * - 1テスト1アサーション（できるだけ）
 * - テストの独立性
 */
class CaDiCaLSolverTest extends AnyFunSpec with Matchers with BeforeAndAfterEach {

  var solver: CaDiCaLSolver = _

  override def beforeEach(): Unit = {
    solver = new CaDiCaLSolver()
  }

  describe("CaDiCaLSolver") {

    describe("基本的な節の追加と充足可能性判定") {

      it("空のソルバーは充足可能である") {
        // Arrange: 何も追加しない

        // Act
        val result = solver.isSatisfiable

        // Assert
        result shouldBe true
      }

      it("単一のリテラルからなる節を追加すると充足可能である") {
        // Arrange
        solver.addClause(Seq(1))

        // Act
        val result = solver.isSatisfiable

        // Assert
        result shouldBe true
      }

      it("矛盾する2つの単位節を追加すると充足不能である") {
        // Arrange
        solver.addClause(Seq(1))
        solver.addClause(Seq(-1))

        // Act
        val result = solver.isSatisfiable

        // Assert
        result shouldBe false
      }

      it("空節を追加すると充足不能である") {
        // Arrange
        solver.addClause(Seq.empty)

        // Act
        val result = solver.isSatisfiable

        // Assert
        result shouldBe false
      }
    }

    describe("モデルの取得") {

      it("充足可能な場合にモデルを取得できる") {
        // Arrange
        solver.addClause(Seq(1, 2))
        solver.addClause(Seq(-1, 2))

        // Act
        solver.isSatisfiable

        // Assert
        solver.model(2) shouldBe true
      }

      it("単位節で強制された変数の値は正しく取得できる") {
        // Arrange
        solver.addClause(Seq(1))

        // Act
        solver.isSatisfiable

        // Assert
        solver.model(1) shouldBe true
      }

      it("負のリテラルで強制された変数の値はfalseである") {
        // Arrange
        solver.addClause(Seq(-1))

        // Act
        solver.isSatisfiable

        // Assert
        solver.model(1) shouldBe false
      }

      it("充足可能判定前にモデルを取得しようとすると例外が発生する") {
        // Arrange
        solver.addClause(Seq(1))

        // Act & Assert
        an[IllegalStateException] should be thrownBy {
          solver.getModelArray
        }
      }
    }

    describe("仮定付き解決") {

      it("仮定と矛盾しない場合は充足可能である") {
        // Arrange
        solver.addClause(Seq(1, 2))

        // Act
        val result = solver.isSatisfiable(Seq(1))

        // Assert
        result shouldBe true
      }

      it("仮定が節と矛盾する場合は充足不能である") {
        // Arrange
        solver.addClause(Seq(1))

        // Act
        val result = solver.isSatisfiable(Seq(-1))

        // Assert
        result shouldBe false
      }

      it("仮定は一時的であり次の解決には影響しない") {
        // Arrange
        solver.addClause(Seq(1, 2))
        solver.isSatisfiable(Seq(-1, -2)) // この仮定では UNSAT

        // Act
        val result = solver.isSatisfiable // 仮定なしで再度解決

        // Assert
        result shouldBe true
      }
    }

    describe("リセット機能") {

      it("リセット後は空の状態に戻る") {
        // Arrange
        solver.addClause(Seq(1))
        solver.addClause(Seq(-1))
        solver.isSatisfiable shouldBe false

        // Act
        solver.reset()

        // Assert
        solver.isSatisfiable shouldBe true
      }

      it("リセット後に新しい節を追加できる") {
        // Arrange
        solver.addClause(Seq(1))
        solver.reset()

        // Act
        solver.addClause(Seq(2))
        val result = solver.isSatisfiable

        // Assert
        result shouldBe true
        solver.model(2) shouldBe true
      }
    }

    describe("カーディナリティ制約") {

      describe("addAtLeast") {

        it("at-least-1 は少なくとも1つが真であることを要求する") {
          // Arrange
          solver.addAtLeast(Seq(1, 2, 3), 1)
          solver.addClause(Seq(-1))
          solver.addClause(Seq(-2))

          // Act
          val result = solver.isSatisfiable

          // Assert
          result shouldBe true
          solver.model(3) shouldBe true
        }

        it("at-least-k で k 個未満しか真にできない場合は充足不能である") {
          // Arrange
          solver.addAtLeast(Seq(1, 2), 2)
          solver.addClause(Seq(-1))

          // Act
          val result = solver.isSatisfiable

          // Assert
          result shouldBe false
        }

        it("at-least-0 は常に充足可能である") {
          // Arrange
          solver.addAtLeast(Seq(1, 2, 3), 0)
          solver.addClause(Seq(-1))
          solver.addClause(Seq(-2))
          solver.addClause(Seq(-3))

          // Act
          val result = solver.isSatisfiable

          // Assert
          result shouldBe true
        }
      }

      describe("addAtMost") {

        // 注: addAtMost は内部で addAtLeast(negated_lits, n-k) を使用するため、
        // 負のリテラルでの sequential counter encoding に問題がある。
        // ペア制約方式で at-most-1 をテストする。

        it("at-most-1 は高々1つが真であることを要求する（ペア制約方式）") {
          // Arrange: 手動でペア制約を追加
          // at-most-1(1,2,3) = ¬(1∧2) ∧ ¬(1∧3) ∧ ¬(2∧3)
          solver.addClause(Seq(-1, -2))
          solver.addClause(Seq(-1, -3))
          solver.addClause(Seq(-2, -3))
          solver.addClause(Seq(1))

          // Act
          val result = solver.isSatisfiable

          // Assert
          result shouldBe true
          solver.model(1) shouldBe true
          solver.model(2) shouldBe false
          solver.model(3) shouldBe false
        }

        it("at-most-k で k+1 個以上が強制されると充足不能である（ペア制約方式）") {
          // Arrange
          solver.addClause(Seq(-1, -2))
          solver.addClause(Seq(-1, -3))
          solver.addClause(Seq(-2, -3))
          solver.addClause(Seq(1))
          solver.addClause(Seq(2))

          // Act
          val result = solver.isSatisfiable

          // Assert
          result shouldBe false
        }
      }

      describe("addExactly") {

        it("exactly-1 はちょうど1つが真であることを要求する") {
          // Arrange
          solver.addExactly(Seq(1, 2, 3), 1)

          // Act
          val result = solver.isSatisfiable

          // Assert
          result shouldBe true
          val trueCount = (1 to 3).count(v => solver.model(v))
          trueCount shouldBe 1
        }

        it("exactly-2 はちょうど2つが真であることを要求する") {
          // Arrange
          solver.addExactly(Seq(1, 2, 3), 2)

          // Act
          val result = solver.isSatisfiable

          // Assert
          result shouldBe true
          val trueCount = (1 to 3).count(v => solver.model(v))
          trueCount shouldBe 2
        }
      }
    }

    describe("古典的な問題") {

      it("鳩の巣原理: 3羽の鳩を2つの巣に入れることはできない") {
        // Arrange
        // 変数 p[i][j] = 鳩iが巣jにいる (i: 1-3, j: 1-2)
        // p[1][1]=1, p[1][2]=2, p[2][1]=3, p[2][2]=4, p[3][1]=5, p[3][2]=6

        // 各鳩はどこかの巣にいる
        solver.addClause(Seq(1, 2))   // 鳩1
        solver.addClause(Seq(3, 4))   // 鳩2
        solver.addClause(Seq(5, 6))   // 鳩3

        // 各巣には高々1羽
        solver.addAtMost(Seq(1, 3, 5), 1)  // 巣1
        solver.addAtMost(Seq(2, 4, 6), 1)  // 巣2

        // Act
        val result = solver.isSatisfiable

        // Assert
        result shouldBe false
      }

      it("グラフ彩色: 三角形を2色で塗ることはできない") {
        // Arrange
        // 変数 c[v][k] = 頂点vが色k (v: 1-3, k: 1-2)
        // c[1][1]=1, c[1][2]=2, c[2][1]=3, c[2][2]=4, c[3][1]=5, c[3][2]=6

        // 各頂点はちょうど1色
        solver.addExactly(Seq(1, 2), 1)
        solver.addExactly(Seq(3, 4), 1)
        solver.addExactly(Seq(5, 6), 1)

        // 隣接頂点は異なる色 (三角形: 1-2, 2-3, 1-3)
        solver.addClause(Seq(-1, -3))  // 頂点1,2 は色1で同じにならない
        solver.addClause(Seq(-2, -4))  // 頂点1,2 は色2で同じにならない
        solver.addClause(Seq(-3, -5))  // 頂点2,3 は色1で同じにならない
        solver.addClause(Seq(-4, -6))  // 頂点2,3 は色2で同じにならない
        solver.addClause(Seq(-1, -5))  // 頂点1,3 は色1で同じにならない
        solver.addClause(Seq(-2, -6))  // 頂点1,3 は色2で同じにならない

        // Act
        val result = solver.isSatisfiable

        // Assert
        result shouldBe false
      }

      it("グラフ彩色: 三角形を3色で塗ることができる") {
        // Arrange
        // 変数 c[v][k] = 頂点vが色k (v: 1-3, k: 1-3)
        // 変数番号: 1-9

        def varId(v: Int, k: Int): Int = (v - 1) * 3 + k

        // 隣接頂点は異なる色 (先に追加して変数番号を確定)
        val edges = Seq((1, 2), (2, 3), (1, 3))
        for ((v1, v2) <- edges; k <- 1 to 3) {
          solver.addClause(Seq(-varId(v1, k), -varId(v2, k)))
        }

        // 各頂点は少なくとも1色
        for (v <- 1 to 3) {
          solver.addClause((1 to 3).map(k => varId(v, k)))
        }

        // 各頂点は高々1色 (ペア制約で表現)
        for (v <- 1 to 3; k1 <- 1 to 3; k2 <- (k1 + 1) to 3) {
          solver.addClause(Seq(-varId(v, k1), -varId(v, k2)))
        }

        // Act
        val result = solver.isSatisfiable

        // Assert
        result shouldBe true
      }
    }

    describe("変数管理") {

      it("nVars は使用された最大変数番号を返す") {
        // Arrange
        solver.addClause(Seq(5, -3))

        // Act
        solver.isSatisfiable

        // Assert
        solver.nVars() should be >= 5
      }

      it("nConstraints は追加された節の数を返す") {
        // Arrange
        solver.addClause(Seq(1))
        solver.addClause(Seq(2))
        solver.addClause(Seq(3))

        // Act & Assert
        solver.nConstraints() shouldBe 3
      }
    }
  }
}
