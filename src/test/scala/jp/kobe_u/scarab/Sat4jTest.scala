package jp.kobe_u.scarab

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterEach

/**
 * Sat4j ラッパークラスのユニットテスト
 *
 * Scarab が使用する Sat4j API の動作を検証する
 */
class Sat4jTest extends AnyFunSpec with Matchers with BeforeAndAfterEach {

  var solver: Sat4j = _

  override def beforeEach(): Unit = {
    solver = new Sat4j()
  }

  describe("Sat4j") {

    describe("基本的な節の追加と充足可能性判定") {

      it("空のソルバーは充足可能である") {
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
        // 注: Sat4j は矛盾検出時に ContradictionException を投げるが、
        // Sat4j ラッパーは clearlyUNSAT フラグで処理する
        solver.addClause(Seq(1), 0)
        solver.addClause(Seq(-1), 1)

        // Act
        val result = solver.isSatisfiable

        // Assert
        result shouldBe false
      }

      it("3-SAT問題を解くことができる") {
        // Arrange: (x1 ∨ x2 ∨ x3) ∧ (¬x1 ∨ ¬x2) ∧ (¬x2 ∨ x3)
        solver.addClause(Seq(1, 2, 3))
        solver.addClause(Seq(-1, -2))
        solver.addClause(Seq(-2, 3))

        // Act
        val result = solver.isSatisfiable

        // Assert
        result shouldBe true
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

      it("getModelArrayでモデル配列を取得できる") {
        // Arrange
        solver.addClause(Seq(1))
        solver.addClause(Seq(-2))

        // Act
        solver.isSatisfiable
        val model = solver.getModelArray

        // Assert
        model should contain (1)
        model should contain (-2)
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
        solver.addClause(Seq(1), 0)
        solver.addClause(Seq(-1), 1)
        solver.isSatisfiable shouldBe false

        // Act
        solver.reset

        // Assert
        solver.isSatisfiable shouldBe true
      }
    }

    describe("カーディナリティ制約") {

      it("addAtLeast: 少なくともk個が真であることを要求する") {
        // Arrange
        solver.addAtLeast(Seq(1, 2, 3), 2)

        // Act
        val result = solver.isSatisfiable

        // Assert
        result shouldBe true
        val trueCount = (1 to 3).count(v => solver.model(v))
        trueCount should be >= 2
      }

      it("addAtMost: 高々k個が真であることを要求する") {
        // Arrange
        solver.addAtMost(Seq(1, 2, 3), 1)
        solver.addClause(Seq(1)) // 少なくとも1つは真

        // Act
        val result = solver.isSatisfiable

        // Assert
        result shouldBe true
        val trueCount = (1 to 3).count(v => solver.model(v))
        trueCount shouldBe 1
      }

      it("addExactly: ちょうどk個が真であることを要求する") {
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

    describe("タイムアウト設定") {

      it("setTimeoutでタイムアウトを設定できる") {
        // Arrange & Act (例外が発生しなければOK)
        solver.setTimeout(10)

        // Assert
        solver.isSatisfiable shouldBe true
      }
    }

    describe("統計情報") {

      it("nVarsで変数の数を取得できる") {
        // Arrange
        solver.addClause(Seq(1, 2, 3))

        // Act & Assert
        solver.nVars should be >= 3
      }

      it("nConstraintsで制約の数を取得できる") {
        // Arrange
        solver.addClause(Seq(1))
        solver.addClause(Seq(2))
        solver.addClause(Seq(3))

        // Act & Assert
        solver.nConstraints shouldBe 3
      }
    }

    describe("古典的な問題") {

      it("鳩の巣原理: 3羽の鳩を2つの巣に入れることはできない") {
        // Arrange
        // 変数 p[i][j] = 鳩iが巣jにいる (i: 1-3, j: 1-2)

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

      it("グラフ彩色: 三角形を3色で塗ることができる") {
        // Arrange
        def varId(v: Int, k: Int): Int = (v - 1) * 3 + k

        // 各頂点はちょうど1色
        for (v <- 1 to 3) {
          solver.addExactly((1 to 3).map(k => varId(v, k)), 1)
        }

        // 隣接頂点は異なる色
        val edges = Seq((1, 2), (2, 3), (1, 3))
        for ((v1, v2) <- edges; k <- 1 to 3) {
          solver.addClause(Seq(-varId(v1, k), -varId(v2, k)))
        }

        // Act
        val result = solver.isSatisfiable

        // Assert
        result shouldBe true
      }
    }

    describe("ソルバーオプション") {

      it("Iteratorオプションでソルバーを作成できる") {
        // Arrange
        val iterSolver = new Sat4j("Iterator")
        iterSolver.addClause(Seq(1, 2))

        // Act
        val result = iterSolver.isSatisfiable

        // Assert
        result shouldBe true
      }

      it("Dimacsオプションでソルバーを作成できる") {
        // Arrange
        val dimacsSolver = new Sat4j("Dimacs")
        dimacsSolver.addClause(Seq(1, 2))

        // Act & Assert (例外が発生しなければOK)
        dimacsSolver.nConstraints shouldBe 1
      }
    }
  }
}
