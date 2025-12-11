package jp.kobe_u.scarab

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterEach

/**
 * Sat4jPB ラッパークラスのユニットテスト
 *
 * Scarab が使用する Sat4j Pseudo-Boolean API の動作を検証する
 */
class Sat4jPBTest extends AnyFunSpec with Matchers with BeforeAndAfterEach {

  var solver: Sat4jPB = _

  override def beforeEach(): Unit = {
    solver = new Sat4jPB()
  }

  describe("Sat4jPB") {

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

      it("矛盾する2つの単位節を追加すると充足不能またはContradictionExceptionが発生する") {
        // Arrange & Act
        // 注: Sat4jPB は矛盾検出時に ContradictionException を投げる
        // これは正常な動作として、例外発生または isSatisfiable=false を許容する
        import org.sat4j.specs.ContradictionException

        val result = try {
          solver.addClause(Seq(1))
          solver.addClause(Seq(-1))
          solver.isSatisfiable
        } catch {
          case _: ContradictionException => false
        }

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
    }

    describe("リセット機能") {

      it("リセット後は空の状態に戻る") {
        // Arrange
        import org.sat4j.specs.ContradictionException
        try {
          solver.addClause(Seq(1))
          solver.addClause(Seq(-1))
        } catch {
          case _: ContradictionException => // 矛盾が検出された
        }

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

    describe("擬似ブール制約 (Pseudo-Boolean Constraints)") {

      it("addPB: 重み付き制約を追加できる") {
        // Arrange: 2*x1 + 3*x2 + 1*x3 >= 4
        solver.addPB(Seq(1, 2, 3), Seq(2, 3, 1), 4)

        // Act
        val result = solver.isSatisfiable

        // Assert
        result shouldBe true
        val weightedSum = (if (solver.model(1)) 2 else 0) +
                          (if (solver.model(2)) 3 else 0) +
                          (if (solver.model(3)) 1 else 0)
        weightedSum should be >= 4
      }

      it("addPB: 重み付き制約が満たせない場合は充足不能またはContradictionExceptionが発生する") {
        // Arrange: 2*x1 + 3*x2 >= 6 かつ ¬x1 かつ ¬x2
        // 注: Sat4jPB は矛盾検出時に ContradictionException を投げることがある
        import org.sat4j.specs.ContradictionException

        val result = try {
          solver.addPB(Seq(1, 2), Seq(2, 3), 6)
          solver.addClause(Seq(-1))
          solver.addClause(Seq(-2))
          solver.isSatisfiable
        } catch {
          case _: ContradictionException => false
        }

        // Assert
        result shouldBe false
      }

      it("addPB: ナップサック問題を解くことができる") {
        // Arrange: 品物の重さ [3, 4, 2, 5], 容量 7
        // 重さの合計が7以下になるように選ぶ
        // ¬(重さ > 7) = 重さ <= 7 = ¬x1*3 + ¬x2*4 + ¬x3*2 + ¬x4*5 >= 14-7
        // これは addAtMost で表現可能だが、PB制約として:
        // 3*x1 + 4*x2 + 2*x3 + 5*x4 <= 7
        // = -3*x1 + -4*x2 + -2*x3 + -5*x4 >= -7
        // = 3*¬x1 + 4*¬x2 + 2*¬x3 + 5*¬x4 >= 7 (変換: x -> 1-¬x)

        // 簡略化: 全部選ぶと 3+4+2+5=14 > 7 なので不可能
        // x1, x3 を選ぶと 3+2=5 <= 7 で可能
        solver.addPB(Seq(-1, -2, -3, -4), Seq(3, 4, 2, 5), 7)

        // 少なくとも2つは選ぶ
        solver.addAtLeast(Seq(1, 2, 3, 4), 2)

        // Act
        val result = solver.isSatisfiable

        // Assert
        result shouldBe true
        val selectedWeight = (if (solver.model(1)) 3 else 0) +
                             (if (solver.model(2)) 4 else 0) +
                             (if (solver.model(3)) 2 else 0) +
                             (if (solver.model(4)) 5 else 0)
        selectedWeight should be <= 7
        val selectedCount = (1 to 4).count(v => solver.model(v))
        selectedCount should be >= 2
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

      it("nextFreeVarIDで次の自由変数IDを取得できる") {
        // Arrange
        solver.addClause(Seq(1, 2, 3))

        // Act
        val nextId = solver.nextFreeVarID(false)

        // Assert
        nextId should be >= 4
      }
    }

    describe("古典的な問題") {

      it("鳩の巣原理: 3羽の鳩を2つの巣に入れることはできない") {
        // Arrange
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

      it("0-1整数計画問題: 予算内での利益最大化の実行可能性を確認できる") {
        // Arrange: プロジェクト選択問題
        // 予算: 10, 各プロジェクトのコスト [3, 5, 4, 6]
        // 少なくとも総コスト10以上のプロジェクトは選べない条件と、
        // 少なくとも2つは選ぶ条件

        // コストの合計が10以下: 3*¬x1 + 5*¬x2 + 4*¬x3 + 6*¬x4 >= 8
        solver.addPB(Seq(-1, -2, -3, -4), Seq(3, 5, 4, 6), 8)

        // 少なくとも2つ選ぶ
        solver.addAtLeast(Seq(1, 2, 3, 4), 2)

        // Act
        val result = solver.isSatisfiable

        // Assert
        result shouldBe true
      }
    }
  }
}
