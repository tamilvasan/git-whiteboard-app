package graph

import components.CommitCircle
import components.Line
import components.Renderable
import config.GitGraphConfiguration
import fabricjs.FabricCanvas
import fabricjs.Point

class Commit(var id: String, linePosition: Int, val swimlane: Int, val parent: Commit? = null, commitColor: String = "") : Renderable {
    var isLostInReflog = false
        set(value) {
            commitCircle.isLostInReflog = value
            field = value
        }

    var mergedCommit: Commit? = null
    val branches = mutableSetOf<AbstractBranch>()
    val commitCircle = CommitCircle(
        id, Point(
            GitGraphConfiguration.leftOffset + swimlane * GitGraphConfiguration.swimlaneDistance,
            GitGraphConfiguration.bottomOffset - linePosition * GitGraphConfiguration.commitDistance
        ),
        commitColor
    )
    var parentLine: Line? = null
    var mergedParentLine: Line? = null

    fun addBranch(branch: AbstractBranch) = branches.add(branch)
    fun removeBranch(branch: AbstractBranch) = branches.remove(branch)

    override fun render(canvas: FabricCanvas) {
        commitCircle.render(canvas)
        val parent = parent
        // draw line to parent commit(s)
        if (parent != null) {
            if (swimlane == parent.swimlane) {
                parentLine = Line(
                    parent.commitCircle.getUpperDockPoint(),
                    commitCircle.getLowerDockPoint()
                )
                parentLine?.render(canvas)
            } else {
                parentLine = Line(
                    parent.commitCircle.getRightDockPoint(),
                    commitCircle.getLowerDockPoint()
                )
                parentLine?.render(canvas)
            }
            val mergedCommit = mergedCommit
            if (mergedCommit != null) {
                mergedParentLine = Line(mergedCommit.commitCircle.getUpperDockPoint(), commitCircle.getLowerDockPoint())
                mergedParentLine?.render(canvas)
            }
        }
    }

    override fun removeFrom(canvas: FabricCanvas) {
        commitCircle.removeFrom(canvas)
        parentLine?.removeFrom(canvas)
        mergedParentLine?.removeFrom(canvas)
    }

    override fun toString(): String = id

    fun repositionBranches(canvas: FabricCanvas) {
        val branchesCopy = ArrayList(branches)
        branches.clear()
        branchesCopy.forEach { branch ->
            branch.attachToCommit(this, canvas)
        }
    }

    fun isAncestorOf(commit: Commit): Boolean = commit.isParentCommit(this)

    private fun isParentCommit(commit: Commit) : Boolean{
        if (parent == null) {
            // reached first commit in history
            return false
        }
        return if (parent == commit || mergedCommit == commit) {
            true
        } else {
            parent.isParentCommit(commit) || (mergedCommit?.isParentCommit(commit) ?: false)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class.js != other::class.js) return false

        other as Commit

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
